package com.techstore.order.service.impl;

import com.techstore.auth.entity.Role;
import com.techstore.auth.entity.User;
import com.techstore.auth.repository.UserRepository;
import com.techstore.cart.entity.Cart;
import com.techstore.cart.entity.CartItem;
import com.techstore.cart.repository.CartRepository;
import com.techstore.order.dto.request.CreateOrderRequest;
import com.techstore.order.dto.response.OrderDetailResponse;
import com.techstore.order.dto.response.OrderItemResponse;
import com.techstore.order.dto.response.OrderSummaryResponse;
import com.techstore.order.dto.response.OrderTimelineResponse;
import com.techstore.order.dto.response.OrderTrackingResponse;
import com.techstore.order.entity.Order;
import com.techstore.order.entity.OrderItem;
import com.techstore.order.entity.OrderPaymentStatus;
import com.techstore.order.entity.OrderStatus;
import com.techstore.order.entity.OrderStatusHistory;
import com.techstore.order.entity.PaymentMethod;
import com.techstore.order.exception.OrderErrorCode;
import com.techstore.order.exception.OrderException;
import com.techstore.order.repository.OrderRepository;
import com.techstore.order.repository.OrderStatusHistoryRepository;
import com.techstore.order.service.OrderService;
import com.techstore.order.service.OrderStatusHistoryService;
import com.techstore.payment.entity.Payment;
import com.techstore.payment.entity.PaymentStatus;
import com.techstore.payment.repository.PaymentRepository;
import com.techstore.product.entity.Product;
import com.techstore.product.repository.ProductRepository;
import java.time.Clock;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderServiceImpl implements OrderService {

    private static final DateTimeFormatter CODE_DATE =
            DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final OrderStatusHistoryService historyService;
    private final Clock clock;

    public OrderServiceImpl(
            OrderRepository orderRepository,
            CartRepository cartRepository,
            ProductRepository productRepository,
            UserRepository userRepository,
            PaymentRepository paymentRepository,
            OrderStatusHistoryRepository historyRepository,
            OrderStatusHistoryService historyService,
            Clock clock
    ) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.paymentRepository = paymentRepository;
        this.historyRepository = historyRepository;
        this.historyService = historyService;
        this.clock = clock;
    }

    @Override
    @Transactional
    public OrderDetailResponse createOrder(
            Long userId,
            CreateOrderRequest request
    ) {
        User user = findUser(userId);
        Cart cart = cartRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new OrderException(OrderErrorCode.CART_EMPTY));
        if (cart.getItems().isEmpty()) {
            throw new OrderException(OrderErrorCode.CART_EMPTY);
        }

        List<Long> productIds = cart.getItems().stream()
                .map(item -> item.getProduct().getId())
                .sorted()
                .toList();
        Map<Long, Product> products = lockProducts(productIds);
        Order order = new Order(
                generateOrderCode(),
                user,
                valueOrDefault(
                        request == null ? null : request.recipientName(),
                        user.getFullName()
                ),
                valueOrDefault(
                        request == null ? null : request.phone(),
                        user.getPhone()
                ),
                normalize(request == null ? null : request.shippingAddress()),
                normalize(request == null ? null : request.note()),
                request == null || request.paymentMethod() == null
                        ? PaymentMethod.COD
                        : request.paymentMethod()
        );

        for (CartItem cartItem : cart.getItems()) {
            Product product = products.get(cartItem.getProduct().getId());
            validateProduct(product, cartItem.getQuantity());
            product.decreaseStock(cartItem.getQuantity());
            order.addItem(product, cartItem.getQuantity());
        }

        productRepository.saveAll(products.values());
        historyService.recordCreated(order, user, clock.instant());
        Order saved = orderRepository.saveAndFlush(order);
        cart.clear();
        cartRepository.saveAndFlush(cart);
        return toDetail(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderSummaryResponse> getOrders(Long userId) {
        User user = findUser(userId);
        List<Order> orders = isManager(user)
                ? orderRepository.findAllByOrderByCreatedAtDesc()
                : orderRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
        return orders.stream().map(this::toSummary).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDetailResponse getOrder(Long userId, Long orderId) {
        User user = findUser(userId);
        Order order = orderRepository.findDetailById(orderId)
                .orElseThrow(() -> new OrderException(OrderErrorCode.ORDER_NOT_FOUND));
        verifyAccess(user, order);
        return toDetail(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderByCode(Long userId, String orderCode) {
        User user = findUser(userId);
        Order order = orderRepository.findByOrderCodeIgnoreCase(orderCode.trim())
                .orElseThrow(() -> new OrderException(
                        OrderErrorCode.ORDER_NOT_FOUND
                ));
        verifyAccess(user, order);
        return toDetail(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderTrackingResponse getTracking(Long userId, Long orderId) {
        User user = findUser(userId);
        Order order = orderRepository.findDetailById(orderId)
                .orElseThrow(() -> new OrderException(
                        OrderErrorCode.ORDER_NOT_FOUND
                ));
        verifyAccess(user, order);
        Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);
        List<OrderTimelineResponse> timeline = timeline(order);
        return new OrderTrackingResponse(
                order.getId(),
                order.getOrderCode(),
                order.getRecipientName(),
                order.getPhone(),
                order.getShippingAddress(),
                order.getNote(),
                order.getPaymentMethod(),
                paymentStatus(order, payment),
                payment == null ? null : payment.getMomoTransactionId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getItems().stream().map(this::toItemResponse).toList(),
                timeline,
                cancellationReason(timeline),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderTimelineResponse> getTimeline(
            Long userId,
            Long orderId
    ) {
        User user = findUser(userId);
        Order order = orderRepository.findDetailById(orderId)
                .orElseThrow(() -> new OrderException(
                        OrderErrorCode.ORDER_NOT_FOUND
                ));
        verifyAccess(user, order);
        return timeline(order);
    }

    @Override
    @Transactional
    public OrderDetailResponse cancelOrder(
            Long userId,
            Long orderId,
            String reason
    ) {
        User user = findUser(userId);
        Order order = findOrderForUpdate(orderId);
        verifyAccess(user, order);
        if (!canCancel(user, order.getStatus())) {
            throw new OrderException(OrderErrorCode.INVALID_STATUS_TRANSITION);
        }

        restoreStock(order);
        OrderStatus oldStatus = order.getStatus();
        order.cancel(clock.instant());
        historyService.record(
                order,
                oldStatus,
                valueOrDefault(reason, "Order cancelled"),
                clock.instant()
        );
        paymentRepository.findByOrderIdForUpdate(orderId).ifPresent(payment -> {
            payment.cancelWithOrder();
            paymentRepository.save(payment);
        });
        return toDetail(orderRepository.saveAndFlush(order));
    }

    @Override
    @Transactional
    public OrderDetailResponse confirmOrder(Long userId, Long orderId) {
        requireManager(findUser(userId));
        Order order = findOrderForUpdate(orderId);
        if (order.getStatus() != OrderStatus.PENDING
                && order.getStatus() != OrderStatus.PAID) {
            throw new OrderException(OrderErrorCode.INVALID_STATUS_TRANSITION);
        }
        OrderStatus oldStatus = order.getStatus();
        order.confirm(clock.instant());
        historyService.record(
                order,
                oldStatus,
                "Order confirmed by staff",
                clock.instant()
        );
        return toDetail(orderRepository.saveAndFlush(order));
    }

    @Override
    @Transactional
    public OrderDetailResponse startShipping(Long userId, Long orderId) {
        requireManager(findUser(userId));
        Order order = findOrderForUpdate(orderId);
        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new OrderException(OrderErrorCode.INVALID_STATUS_TRANSITION);
        }
        OrderStatus oldStatus = order.getStatus();
        order.startShipping(clock.instant());
        historyService.record(
                order,
                oldStatus,
                "Order handed to the delivery service",
                clock.instant()
        );
        return toDetail(orderRepository.saveAndFlush(order));
    }

    @Override
    @Transactional
    public OrderDetailResponse markDelivered(Long userId, Long orderId) {
        requireManager(findUser(userId));
        Order order = findOrderForUpdate(orderId);
        if (order.getStatus() != OrderStatus.SHIPPING) {
            throw new OrderException(OrderErrorCode.INVALID_STATUS_TRANSITION);
        }
        OrderStatus oldStatus = order.getStatus();
        order.markDelivered(clock.instant());
        historyService.record(
                order,
                oldStatus,
                "Order delivered to the customer",
                clock.instant()
        );
        return toDetail(orderRepository.saveAndFlush(order));
    }

    private void restoreStock(Order order) {
        List<Long> productIds = order.getItems().stream()
                .map(item -> item.getProduct().getId())
                .sorted()
                .toList();
        Map<Long, Product> products = lockProducts(productIds);
        order.getItems().forEach(item -> {
            Product product = products.get(item.getProduct().getId());
            if (product == null) {
                throw new OrderException(OrderErrorCode.PRODUCT_UNAVAILABLE);
            }
            product.restoreStock(item.getQuantity());
        });
        productRepository.saveAll(products.values());
    }

    private Map<Long, Product> lockProducts(Collection<Long> productIds) {
        List<Product> locked = productRepository.findAllByIdInForUpdate(productIds)
                .stream()
                .sorted(Comparator.comparing(Product::getId))
                .toList();
        Map<Long, Product> products = new HashMap<>();
        locked.forEach(product -> products.put(product.getId(), product));
        if (products.size() != productIds.size()) {
            throw new OrderException(OrderErrorCode.PRODUCT_UNAVAILABLE);
        }
        return products;
    }

    private void validateProduct(Product product, int quantity) {
        if (product == null || product.isDeleted()) {
            throw new OrderException(OrderErrorCode.PRODUCT_UNAVAILABLE);
        }
        if (quantity > product.getStockQuantity()) {
            throw new OrderException(OrderErrorCode.INSUFFICIENT_STOCK);
        }
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new OrderException(OrderErrorCode.USER_NOT_FOUND));
    }

    private Order findOrderForUpdate(Long orderId) {
        return orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new OrderException(OrderErrorCode.ORDER_NOT_FOUND));
    }

    private void verifyAccess(User user, Order order) {
        if (!isManager(user) && !order.getUser().getId().equals(user.getId())) {
            throw new OrderException(OrderErrorCode.ORDER_ACCESS_DENIED);
        }
    }

    private void requireManager(User user) {
        if (!isManager(user)) {
            throw new OrderException(OrderErrorCode.ORDER_ACCESS_DENIED);
        }
    }

    private boolean isManager(User user) {
        return user.getRole() == Role.ROLE_ADMIN || user.getRole() == Role.ROLE_STAFF;
    }

    private boolean canCancel(User user, OrderStatus status) {
        if (isManager(user)) {
            return status == OrderStatus.PENDING
                    || status == OrderStatus.PENDING_PAYMENT
                    || status == OrderStatus.PAID
                    || status == OrderStatus.CONFIRMED;
        }
        return status == OrderStatus.PENDING
                || status == OrderStatus.PENDING_PAYMENT;
    }

    private String generateOrderCode() {
        String code;
        do {
            String suffix = UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .substring(0, 10)
                    .toUpperCase(Locale.ROOT);
            code = "ORD-" + CODE_DATE.format(clock.instant()) + "-" + suffix;
        } while (orderRepository.existsByOrderCode(code));
        return code;
    }

    private OrderSummaryResponse toSummary(Order order) {
        return new OrderSummaryResponse(
                order.getId(),
                order.getOrderCode(),
                order.getUser().getId(),
                order.getUser().getFullName(),
                order.getStatus(),
                order.getPaymentMethod(),
                paymentStatus(order, paymentRepository
                        .findByOrderId(order.getId())
                        .orElse(null)),
                order.getTotalQuantity(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    private OrderDetailResponse toDetail(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(this::toItemResponse)
                .toList();
        return new OrderDetailResponse(
                order.getId(),
                order.getOrderCode(),
                order.getUser().getId(),
                order.getUser().getFullName(),
                order.getUser().getEmail(),
                order.getRecipientName(),
                order.getPhone(),
                order.getShippingAddress(),
                order.getNote(),
                order.getStatus(),
                order.getPaymentMethod(),
                paymentStatus(order, paymentRepository
                        .findByOrderId(order.getId())
                        .orElse(null)),
                paymentRepository.findByOrderId(order.getId())
                        .map(Payment::getMomoTransactionId)
                        .orElse(null),
                order.getTotalQuantity(),
                order.getTotalAmount(),
                items,
                order.getConfirmedAt(),
                order.getShippedAt(),
                order.getDeliveredAt(),
                order.getCancelledAt(),
                order.getCompletedAt(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getProductName(),
                item.getProductSku(),
                item.getThumbnailUrl(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getSubtotal()
        );
    }

    private List<OrderTimelineResponse> timeline(Order order) {
        List<OrderStatusHistory> history = historyRepository
                .findAllByOrderIdOrderByCreatedAtAscIdAsc(order.getId());
        if (history.isEmpty()) {
            return List.of(toTimeline(
                    order.getStatus(),
                    null,
                    null,
                    order.getCreatedAt()
            ));
        }
        return history.stream()
                .map(item -> toTimeline(
                        item.getNewStatus(),
                        item.getNote(),
                        item.getChangedBy(),
                        item.getCreatedAt()
                ))
                .toList();
    }

    private OrderTimelineResponse toTimeline(
            OrderStatus status,
            String note,
            User changedBy,
            java.time.Instant time
    ) {
        return new OrderTimelineResponse(
                status,
                statusTitle(status),
                statusDescription(status),
                note,
                changedBy == null ? null : changedBy.getId(),
                changedBy == null ? null : changedBy.getFullName(),
                time
        );
    }

    private String statusTitle(OrderStatus status) {
        return switch (status) {
            case PENDING -> "Order placed";
            case PENDING_PAYMENT -> "Awaiting payment";
            case PAID -> "Payment received";
            case CONFIRMED -> "Order confirmed";
            case SHIPPING -> "Out for delivery";
            case DELIVERED -> "Delivered";
            case COMPLETED -> "Completed";
            case CANCELLED -> "Cancelled";
        };
    }

    private String statusDescription(OrderStatus status) {
        return switch (status) {
            case PENDING -> "The order was created and is awaiting confirmation.";
            case PENDING_PAYMENT -> "The order is waiting for MoMo payment.";
            case PAID -> "MoMo payment was completed successfully.";
            case CONFIRMED -> "The store confirmed the order.";
            case SHIPPING -> "The order is on its way to the customer.";
            case DELIVERED -> "The order reached the delivery address.";
            case COMPLETED -> "The order was completed.";
            case CANCELLED -> "The order was cancelled.";
        };
    }

    private OrderPaymentStatus paymentStatus(Order order, Payment payment) {
        if (payment == null) {
            return order.getPaymentMethod() == PaymentMethod.MOMO
                    ? OrderPaymentStatus.PENDING
                    : OrderPaymentStatus.UNPAID;
        }
        return switch (payment.getStatus()) {
            case PENDING -> OrderPaymentStatus.PENDING;
            case PAID -> OrderPaymentStatus.PAID;
            case FAILED -> OrderPaymentStatus.FAILED;
            case CANCELLED -> OrderPaymentStatus.CANCELLED;
            case REFUNDED -> OrderPaymentStatus.REFUNDED;
        };
    }

    private String cancellationReason(List<OrderTimelineResponse> timeline) {
        return timeline.stream()
                .filter(item -> item.status() == OrderStatus.CANCELLED)
                .reduce((first, second) -> second)
                .map(OrderTimelineResponse::note)
                .orElse(null);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String valueOrDefault(String value, String fallback) {
        String normalized = normalize(value);
        return normalized == null ? fallback : normalized;
    }
}
