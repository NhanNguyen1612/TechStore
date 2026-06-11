package com.techstore.admin.service.impl;

import com.techstore.admin.dto.request.AdminRequests;
import com.techstore.admin.dto.response.AdminResponses;
import com.techstore.admin.entity.Coupon;
import com.techstore.admin.entity.CouponType;
import com.techstore.admin.entity.Notification;
import com.techstore.admin.exception.AdminErrorCode;
import com.techstore.admin.exception.AdminException;
import com.techstore.admin.repository.AdminQueryRepository;
import com.techstore.admin.repository.CouponRepository;
import com.techstore.admin.repository.NotificationRepository;
import com.techstore.admin.service.AdminManagementService;
import com.techstore.auth.entity.Role;
import com.techstore.auth.entity.User;
import com.techstore.auth.repository.UserRepository;
import com.techstore.brand.dto.request.CreateBrandRequest;
import com.techstore.brand.dto.request.UpdateBrandRequest;
import com.techstore.brand.dto.response.BrandPageResponse;
import com.techstore.brand.dto.response.BrandResponse;
import com.techstore.brand.service.BrandService;
import com.techstore.category.dto.request.CreateCategoryRequest;
import com.techstore.category.dto.request.UpdateCategoryRequest;
import com.techstore.category.dto.response.CategoryResponse;
import com.techstore.category.dto.response.PageResponse;
import com.techstore.category.service.CategoryService;
import com.techstore.chat.entity.Conversation;
import com.techstore.chat.entity.Message;
import com.techstore.chat.repository.ConversationRepository;
import com.techstore.chat.repository.MessageRepository;
import com.techstore.order.entity.Order;
import com.techstore.order.entity.OrderItem;
import com.techstore.order.entity.OrderStatus;
import com.techstore.order.repository.OrderRepository;
import com.techstore.order.service.OrderStatusHistoryService;
import com.techstore.payment.entity.MomoCallback;
import com.techstore.payment.entity.Payment;
import com.techstore.payment.entity.PaymentStatus;
import com.techstore.payment.entity.PaymentTransaction;
import com.techstore.payment.repository.MomoCallbackRepository;
import com.techstore.payment.repository.PaymentRepository;
import com.techstore.payment.repository.PaymentTransactionRepository;
import com.techstore.product.dto.request.CreateProductRequest;
import com.techstore.product.dto.request.UpdateProductRequest;
import com.techstore.product.entity.Product;
import com.techstore.product.entity.ProductImage;
import com.techstore.product.repository.ProductRepository;
import com.techstore.product.service.ProductService;
import com.techstore.review.entity.Review;
import com.techstore.review.entity.ReviewStatus;
import com.techstore.review.repository.ReviewRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AdminManagementServiceImpl implements AdminManagementService {

    private static final Set<String> COMMON_SORTS =
            Set.of("id", "createdAt", "updatedAt");
    private static final Set<String> USER_SORTS =
            Set.of("id", "email", "fullName", "role", "enabled", "createdAt", "updatedAt");
    private static final Set<String> PRODUCT_SORTS =
            Set.of("id", "name", "sku", "price", "stockQuantity", "soldCount",
                    "active", "createdAt", "updatedAt");
    private static final Set<String> ORDER_SORTS =
            Set.of("id", "orderCode", "status", "totalAmount", "createdAt", "updatedAt");
    private static final Set<String> PAYMENT_SORTS =
            Set.of("id", "status", "amount", "createdAt", "updatedAt");

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderStatusHistoryService orderHistoryService;
    private final PaymentRepository paymentRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final MomoCallbackRepository callbackRepository;
    private final ReviewRepository reviewRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final CouponRepository couponRepository;
    private final NotificationRepository notificationRepository;
    private final AdminQueryRepository queryRepository;
    private final ProductService productService;
    private final CategoryService categoryService;
    private final BrandService brandService;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public AdminManagementServiceImpl(
            UserRepository userRepository,
            ProductRepository productRepository,
            OrderRepository orderRepository,
            OrderStatusHistoryService orderHistoryService,
            PaymentRepository paymentRepository,
            PaymentTransactionRepository transactionRepository,
            MomoCallbackRepository callbackRepository,
            ReviewRepository reviewRepository,
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            CouponRepository couponRepository,
            NotificationRepository notificationRepository,
            AdminQueryRepository queryRepository,
            ProductService productService,
            CategoryService categoryService,
            BrandService brandService,
            PasswordEncoder passwordEncoder,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.orderHistoryService = orderHistoryService;
        this.paymentRepository = paymentRepository;
        this.transactionRepository = transactionRepository;
        this.callbackRepository = callbackRepository;
        this.reviewRepository = reviewRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.couponRepository = couponRepository;
        this.notificationRepository = notificationRepository;
        this.queryRepository = queryRepository;
        this.productService = productService;
        this.categoryService = categoryService;
        this.brandService = brandService;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminResponses.PageData<AdminResponses.UserData> users(
            String search, Role role, Boolean active, int page, int size, String sort
    ) {
        return AdminResponses.PageData.from(queryRepository
                .users(search, role, active, pageable(page, size, sort, USER_SORTS))
                .map(this::toUser));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminResponses.UserData user(Long id) {
        return toUser(findUser(id));
    }

    @Override
    @Transactional
    public AdminResponses.UserData createUser(AdminRequests.CreateUser request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw error(AdminErrorCode.EMAIL_EXISTS, "Email is already in use");
        }
        User user = new User(
                email,
                passwordEncoder.encode(request.password()),
                request.fullName().trim(),
                normalizeNullable(request.phone()),
                request.role()
        );
        if (!request.enabled()) {
            user.changeStatus(false);
        }
        return toUser(userRepository.saveAndFlush(user));
    }

    @Override
    @Transactional
    public AdminResponses.UserData updateUser(
            Long id, AdminRequests.UpdateUser request
    ) {
        User user = findUser(id);
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw error(AdminErrorCode.EMAIL_EXISTS, "Email is already in use");
        }
        user.adminUpdate(
                email,
                request.fullName().trim(),
                normalizeNullable(request.phone())
        );
        return toUser(userRepository.saveAndFlush(user));
    }

    @Override
    @Transactional
    public AdminResponses.UserData updateUserStatus(Long id, boolean active) {
        User user = findUser(id);
        user.changeStatus(active);
        return toUser(userRepository.saveAndFlush(user));
    }

    @Override
    @Transactional
    public AdminResponses.UserData updateUserRole(Long id, Role role) {
        User user = findUser(id);
        user.changeRole(role);
        return toUser(userRepository.saveAndFlush(user));
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        User user = findUser(id);
        user.softDelete(clock.instant());
        userRepository.saveAndFlush(user);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminResponses.PageData<AdminResponses.ProductData> products(
            String search, Long categoryId, Long brandId, Boolean active,
            int page, int size, String sort
    ) {
        return AdminResponses.PageData.from(queryRepository
                .products(
                        search,
                        categoryId,
                        brandId,
                        active,
                        pageable(page, size, sort, PRODUCT_SORTS)
                )
                .map(this::toProduct));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminResponses.ProductData product(Long id) {
        return toProduct(findProduct(id));
    }

    @Override
    @Transactional
    public AdminResponses.ProductData createProduct(
            CreateProductRequest request, List<MultipartFile> images
    ) {
        Long id = productService.createProduct(request, safeFiles(images)).id();
        return toProduct(findProduct(id));
    }

    @Override
    @Transactional
    public AdminResponses.ProductData updateProduct(
            Long id, UpdateProductRequest request, List<MultipartFile> images
    ) {
        productService.updateProduct(id, request, safeFiles(images));
        return toProduct(findProduct(id));
    }

    @Override
    @Transactional
    public AdminResponses.ProductData updateStock(Long id, int stock) {
        Product product = findProduct(id);
        product.updateStock(stock);
        return toProduct(productRepository.saveAndFlush(product));
    }

    @Override
    @Transactional
    public AdminResponses.ProductData updateProductStatus(Long id, boolean active) {
        Product product = findProduct(id);
        product.changeStatus(active);
        return toProduct(productRepository.saveAndFlush(product));
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        productService.deleteProduct(id);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminResponses.PageData<AdminResponses.TaxonomyData> categories(
            String search, int page, int size, String sort
    ) {
        SortRequest parsed = sort(sort, COMMON_SORTS);
        PageResponse<CategoryResponse> result = categoryService.getCategories(
                search,
                page,
                size,
                parsed.property(),
                parsed.direction().name()
        );
        return new AdminResponses.PageData<>(
                result.content().stream().map(this::toTaxonomy).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AdminResponses.TaxonomyData category(Long id) {
        return toTaxonomy(categoryService.getCategory(id));
    }

    @Override
    @Transactional
    public AdminResponses.TaxonomyData createCategory(CreateCategoryRequest request) {
        return toTaxonomy(categoryService.createCategory(request));
    }

    @Override
    @Transactional
    public AdminResponses.TaxonomyData updateCategory(
            Long id, UpdateCategoryRequest request
    ) {
        return toTaxonomy(categoryService.updateCategory(id, request));
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        if (productRepository.countByCategoryIdAndDeletedFalse(id) > 0) {
            throw error(
                    AdminErrorCode.TAXONOMY_IN_USE,
                    "Category is used by active products"
            );
        }
        categoryService.deleteCategory(id);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminResponses.PageData<AdminResponses.TaxonomyData> brands(
            String search, int page, int size, String sort
    ) {
        SortRequest parsed = sort(sort, COMMON_SORTS);
        BrandPageResponse result = brandService.getBrands(
                search,
                page,
                size,
                parsed.property(),
                parsed.direction().name()
        );
        return new AdminResponses.PageData<>(
                result.content().stream().map(this::toTaxonomy).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AdminResponses.TaxonomyData brand(Long id) {
        return toTaxonomy(brandService.getBrand(id));
    }

    @Override
    @Transactional
    public AdminResponses.TaxonomyData createBrand(CreateBrandRequest request) {
        return toTaxonomy(brandService.createBrand(request));
    }

    @Override
    @Transactional
    public AdminResponses.TaxonomyData updateBrand(
            Long id, UpdateBrandRequest request
    ) {
        return toTaxonomy(brandService.updateBrand(id, request));
    }

    @Override
    @Transactional
    public void deleteBrand(Long id) {
        if (productRepository.countByBrandIdAndDeletedFalse(id) > 0) {
            throw error(
                    AdminErrorCode.TAXONOMY_IN_USE,
                    "Brand is used by active products"
            );
        }
        brandService.deleteBrand(id);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminResponses.PageData<AdminResponses.OrderData> orders(
            OrderStatus status, String paymentMethod, Instant from, Instant to,
            int page, int size, String sort
    ) {
        validateRange(from, to);
        return AdminResponses.PageData.from(queryRepository
                .orders(
                        status,
                        paymentMethod,
                        from,
                        to,
                        pageable(page, size, sort, ORDER_SORTS)
                )
                .map(this::toOrder));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminResponses.OrderData order(Long id) {
        return toOrder(orderRepository.findDetailById(id)
                .orElseThrow(() -> notFound("Order")));
    }

    @Override
    @Transactional
    public AdminResponses.OrderData transitionOrder(Long id, OrderStatus target) {
        Order order = orderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> notFound("Order"));
        Instant now = clock.instant();
        OrderStatus oldStatus = order.getStatus();
        switch (target) {
            case CONFIRMED -> {
                requireStatus(order, OrderStatus.PENDING, OrderStatus.PAID);
                order.confirm(now);
            }
            case SHIPPING -> {
                requireStatus(order, OrderStatus.CONFIRMED);
                order.startShipping(now);
            }
            case DELIVERED -> {
                requireStatus(order, OrderStatus.SHIPPING);
                order.markDelivered(now);
            }
            case COMPLETED -> {
                requireStatus(order, OrderStatus.DELIVERED);
                recordSoldCounts(order);
                order.complete(now);
            }
            case CANCELLED -> {
                requireStatus(
                        order,
                        OrderStatus.PENDING,
                        OrderStatus.PENDING_PAYMENT,
                        OrderStatus.PAID,
                        OrderStatus.CONFIRMED
                );
                restoreStock(order);
                order.cancel(now);
                paymentRepository.findByOrderIdForUpdate(id).ifPresent(payment -> {
                    payment.cancelWithOrder();
                    paymentRepository.save(payment);
                });
            }
            default -> throw error(
                    AdminErrorCode.INVALID_ORDER_TRANSITION,
                    "Unsupported target order status"
            );
        }
        orderHistoryService.record(
                order,
                oldStatus,
                adminStatusNote(target),
                now
        );
        return toOrder(orderRepository.saveAndFlush(order));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminResponses.PageData<AdminResponses.PaymentData> payments(
            PaymentStatus status, Instant from, Instant to,
            int page, int size, String sort
    ) {
        validateRange(from, to);
        return AdminResponses.PageData.from(queryRepository
                .payments(
                        status,
                        from,
                        to,
                        pageable(page, size, sort, PAYMENT_SORTS)
                )
                .map(this::toPayment));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminResponses.PaymentData payment(Long id) {
        return toPayment(paymentRepository.findById(id)
                .orElseThrow(() -> notFound("Payment")));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminResponses.PaymentData paymentByOrder(Long orderId) {
        return toPayment(paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> notFound("Payment")));
    }

    @Override
    @Transactional
    public AdminResponses.PaymentData updatePaymentStatus(
            Long id, PaymentStatus status
    ) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> notFound("Payment"));
        Instant now = clock.instant();
        payment.changeStatus(status, now);
        Order order = payment.getOrder();
        OrderStatus oldStatus = order.getStatus();
        if (status == PaymentStatus.PAID
                && order.getStatus() != OrderStatus.CANCELLED) {
            order.markPaid();
            orderHistoryService.record(
                    order,
                    oldStatus,
                    "Payment marked as paid by admin",
                    now
            );
            orderRepository.saveAndFlush(order);
        }
        return toPayment(paymentRepository.saveAndFlush(payment));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminResponses.PageData<AdminResponses.CouponData> coupons(
            String search, Boolean active, int page, int size, String sort
    ) {
        Page<Coupon> result = couponRepository.search(
                search == null ? "" : search.trim(),
                active,
                pageable(page, size, sort, COMMON_SORTS)
        );
        return AdminResponses.PageData.from(result.map(this::toCoupon));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminResponses.CouponData coupon(Long id) {
        return toCoupon(findCoupon(id));
    }

    @Override
    @Transactional
    public AdminResponses.CouponData createCoupon(AdminRequests.UpsertCoupon request) {
        validateCoupon(request);
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        if (couponRepository.existsByCodeIgnoreCaseAndDeletedFalse(code)) {
            throw error(AdminErrorCode.COUPON_EXISTS, "Coupon code already exists");
        }
        Coupon coupon = new Coupon(
                code,
                request.name().trim(),
                request.type(),
                request.value(),
                request.minimumOrderAmount(),
                request.usageLimit(),
                request.startsAt(),
                request.endsAt()
        );
        return toCoupon(couponRepository.saveAndFlush(coupon));
    }

    @Override
    @Transactional
    public AdminResponses.CouponData updateCoupon(
            Long id, AdminRequests.UpsertCoupon request
    ) {
        validateCoupon(request);
        Coupon coupon = findCoupon(id);
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        if (couponRepository.existsByCodeIgnoreCaseAndDeletedFalseAndIdNot(code, id)) {
            throw error(AdminErrorCode.COUPON_EXISTS, "Coupon code already exists");
        }
        coupon.update(
                code,
                request.name().trim(),
                request.type(),
                request.value(),
                request.minimumOrderAmount(),
                request.usageLimit(),
                request.startsAt(),
                request.endsAt()
        );
        return toCoupon(couponRepository.saveAndFlush(coupon));
    }

    @Override
    @Transactional
    public AdminResponses.CouponData updateCouponStatus(Long id, boolean active) {
        Coupon coupon = findCoupon(id);
        coupon.changeStatus(active);
        return toCoupon(couponRepository.saveAndFlush(coupon));
    }

    @Override
    @Transactional
    public void deleteCoupon(Long id) {
        Coupon coupon = findCoupon(id);
        coupon.softDelete(clock.instant());
        couponRepository.saveAndFlush(coupon);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminResponses.PageData<AdminResponses.ReviewData> reviews(
            Integer rating, ReviewStatus status, int page, int size, String sort
    ) {
        if (rating != null && (rating < 1 || rating > 5)) {
            throw error(AdminErrorCode.INVALID_DATE_RANGE, "Rating must be from 1 to 5");
        }
        return AdminResponses.PageData.from(queryRepository
                .reviews(
                        rating,
                        status,
                        pageable(page, size, sort, COMMON_SORTS)
                )
                .map(this::toReview));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminResponses.ReviewData review(Long id) {
        return toReview(reviewRepository.findDetailById(id)
                .orElseThrow(() -> notFound("Review")));
    }

    @Override
    @Transactional
    public AdminResponses.ReviewData moderateReview(
            Long id, ReviewStatus status, Long adminId
    ) {
        Review review = reviewRepository.findDetailById(id)
                .orElseThrow(() -> notFound("Review"));
        if (status == ReviewStatus.APPROVED) {
            review.approve(adminId, clock.instant());
        } else if (status == ReviewStatus.HIDDEN) {
            review.hide();
        } else {
            throw error(
                    AdminErrorCode.INVALID_ORDER_TRANSITION,
                    "Review can only be approved or hidden"
            );
        }
        return toReview(reviewRepository.saveAndFlush(review));
    }

    @Override
    @Transactional
    public void deleteReview(Long id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> notFound("Review"));
        reviewRepository.delete(review);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminResponses.PageData<AdminResponses.ConversationData> conversations(
            Boolean closed, int page, int size, String sort
    ) {
        return AdminResponses.PageData.from(queryRepository
                .conversations(
                        closed,
                        pageable(page, size, sort, COMMON_SORTS)
                )
                .map(this::toConversation));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminResponses.ConversationData conversation(Long id) {
        return toConversation(findConversation(id));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminResponses.PageData<AdminResponses.MessageData> messages(
            Long conversationId, int page, int size
    ) {
        findConversation(conversationId);
        Page<Message> messages = messageRepository.findAllByConversationId(
                conversationId,
                PageRequest.of(page, checkedSize(size), Sort.by(
                        Sort.Direction.DESC,
                        "createdAt"
                ))
        );
        return AdminResponses.PageData.from(messages.map(this::toMessage));
    }

    @Override
    @Transactional
    public AdminResponses.ConversationData assignStaff(
            Long conversationId, Long staffId
    ) {
        Conversation conversation = findConversation(conversationId);
        User staff = findUser(staffId);
        if (staff.getRole() != Role.ROLE_STAFF || !staff.isEnabled()) {
            throw error(
                    AdminErrorCode.INVALID_STAFF,
                    "Assigned user must be an active staff account"
            );
        }
        conversation.assignStaff(staff);
        return toConversation(conversationRepository.saveAndFlush(conversation));
    }

    @Override
    @Transactional
    public AdminResponses.ConversationData closeConversation(Long conversationId) {
        Conversation conversation = findConversation(conversationId);
        conversation.close(clock.instant());
        return toConversation(conversationRepository.saveAndFlush(conversation));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminResponses.PageData<AdminResponses.NotificationData> notifications(
            int page, int size
    ) {
        return AdminResponses.PageData.from(notificationRepository
                .findAll(PageRequest.of(
                        page,
                        checkedSize(size),
                        Sort.by(Sort.Direction.DESC, "createdAt")
                ))
                .map(this::toNotification));
    }

    @Override
    @Transactional
    public AdminResponses.NotificationData createNotification(
            AdminRequests.CreateNotification request, Long adminId
    ) {
        Notification notification = new Notification(
                request.title().trim(),
                request.content().trim(),
                request.targetRole(),
                findUser(adminId)
        );
        return toNotification(notificationRepository.saveAndFlush(notification));
    }

    @Override
    @Transactional
    public void deleteNotification(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> notFound("Notification"));
        notificationRepository.delete(notification);
    }

    private User findUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> notFound("User"));
        if (user.isDeleted()) {
            throw notFound("User");
        }
        return user;
    }

    private Product findProduct(Long id) {
        return productRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> notFound("Product"));
    }

    private Coupon findCoupon(Long id) {
        return couponRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> notFound("Coupon"));
    }

    private Conversation findConversation(Long id) {
        return conversationRepository.findDetailById(id)
                .orElseThrow(() -> notFound("Conversation"));
    }

    private void requireStatus(Order order, OrderStatus... allowed) {
        for (OrderStatus status : allowed) {
            if (order.getStatus() == status) {
                return;
            }
        }
        throw error(
                AdminErrorCode.INVALID_ORDER_TRANSITION,
                "Invalid transition from " + order.getStatus()
        );
    }

    private String adminStatusNote(OrderStatus status) {
        return switch (status) {
            case CONFIRMED -> "Order confirmed by admin";
            case SHIPPING -> "Order marked as shipping by admin";
            case DELIVERED -> "Order marked as delivered by admin";
            case COMPLETED -> "Order completed by admin";
            case CANCELLED -> "Order cancelled by admin";
            default -> "Order status updated by admin";
        };
    }

    private void restoreStock(Order order) {
        List<Long> ids = order.getItems().stream()
                .map(item -> item.getProduct().getId())
                .sorted()
                .toList();
        Map<Long, Product> products = lockProducts(ids);
        order.getItems().forEach(item ->
                products.get(item.getProduct().getId()).restoreStock(item.getQuantity())
        );
        productRepository.saveAll(products.values());
    }

    private void recordSoldCounts(Order order) {
        List<Long> ids = order.getItems().stream()
                .map(item -> item.getProduct().getId())
                .sorted()
                .toList();
        Map<Long, Product> products = lockProducts(ids);
        order.getItems().forEach(item ->
                products.get(item.getProduct().getId())
                        .increaseSoldCount(item.getQuantity())
        );
        productRepository.saveAll(products.values());
    }

    private Map<Long, Product> lockProducts(Collection<Long> ids) {
        List<Product> locked = productRepository.findAllByIdInForUpdate(ids)
                .stream()
                .sorted(Comparator.comparing(Product::getId))
                .toList();
        Map<Long, Product> products = new HashMap<>();
        locked.forEach(product -> products.put(product.getId(), product));
        if (products.size() != ids.size()) {
            throw notFound("Product");
        }
        return products;
    }

    private Pageable pageable(
            int page, int size, String sortValue, Set<String> allowed
    ) {
        SortRequest sort = sort(sortValue, allowed);
        return PageRequest.of(
                Math.max(page, 0),
                checkedSize(size),
                Sort.by(sort.direction(), sort.property())
        );
    }

    private SortRequest sort(String value, Set<String> allowed) {
        String[] parts = value == null ? new String[0] : value.split(",");
        String property = parts.length > 0 && allowed.contains(parts[0])
                ? parts[0]
                : "createdAt";
        Sort.Direction direction = parts.length > 1
                && "asc".equalsIgnoreCase(parts[1])
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        return new SortRequest(property, direction);
    }

    private int checkedSize(int size) {
        return Math.min(Math.max(size, 1), 100);
    }

    private void validateRange(Instant from, Instant to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw error(AdminErrorCode.INVALID_DATE_RANGE, "Invalid date range");
        }
    }

    private void validateCoupon(AdminRequests.UpsertCoupon request) {
        if (!request.endsAt().isAfter(request.startsAt())) {
            throw error(
                    AdminErrorCode.INVALID_COUPON,
                    "Coupon end time must be after start time"
            );
        }
        if (request.type() == CouponType.PERCENTAGE
                && request.value().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw error(
                    AdminErrorCode.INVALID_COUPON,
                    "Percentage coupon must not exceed 100"
            );
        }
    }

    private AdminResponses.UserData toUser(User user) {
        return new AdminResponses.UserData(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getRole(),
                user.isEnabled(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    private AdminResponses.ProductData toProduct(Product product) {
        return new AdminResponses.ProductData(
                product.getId(),
                product.getName(),
                product.getSlug(),
                product.getSku(),
                product.getDescription(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getSoldCount(),
                product.getThumbnailUrl(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getBrand().getId(),
                product.getBrand().getName(),
                product.isActive(),
                product.getImages().stream().map(this::toImage).toList(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    private AdminResponses.ImageData toImage(ProductImage image) {
        return new AdminResponses.ImageData(
                image.getId(),
                image.getUrl(),
                image.getSortOrder(),
                image.isPrimary()
        );
    }

    private AdminResponses.TaxonomyData toTaxonomy(CategoryResponse value) {
        return new AdminResponses.TaxonomyData(
                value.id(), value.name(), value.slug(), value.description(),
                value.createdAt(), value.updatedAt()
        );
    }

    private AdminResponses.TaxonomyData toTaxonomy(BrandResponse value) {
        return new AdminResponses.TaxonomyData(
                value.id(), value.name(), value.slug(), value.description(),
                value.createdAt(), value.updatedAt()
        );
    }

    private AdminResponses.OrderData toOrder(Order order) {
        return new AdminResponses.OrderData(
                order.getId(),
                order.getOrderCode(),
                order.getUser().getId(),
                order.getUser().getFullName(),
                order.getUser().getEmail(),
                order.getStatus(),
                order.getPaymentMethod().name(),
                order.getTotalQuantity(),
                order.getTotalAmount(),
                order.getItems().stream().map(this::toOrderItem).toList(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    private AdminResponses.OrderItemData toOrderItem(OrderItem item) {
        return new AdminResponses.OrderItemData(
                item.getId(),
                item.getProduct().getId(),
                item.getProductName(),
                item.getProductSku(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getSubtotal()
        );
    }

    private AdminResponses.PaymentData toPayment(Payment payment) {
        List<AdminResponses.TransactionData> transactions = transactionRepository
                .findAllByPaymentIdOrderByCreatedAtDesc(payment.getId())
                .stream()
                .map(this::toTransaction)
                .toList();
        List<AdminResponses.CallbackData> callbacks = callbackRepository
                .findAllByPaymentIdOrderByReceivedAtDesc(payment.getId())
                .stream()
                .map(this::toCallback)
                .toList();
        return new AdminResponses.PaymentData(
                payment.getId(),
                payment.getOrder().getId(),
                payment.getOrder().getOrderCode(),
                payment.getProvider(),
                payment.getStatus(),
                payment.getAmount(),
                payment.getRequestId(),
                payment.getMomoOrderId(),
                payment.getMomoTransactionId(),
                payment.getResultCode(),
                payment.getMessage(),
                payment.getPaidAt(),
                payment.getCreatedAt(),
                transactions,
                callbacks
        );
    }

    private AdminResponses.TransactionData toTransaction(PaymentTransaction value) {
        return new AdminResponses.TransactionData(
                value.getId(),
                value.getTransactionType().name(),
                value.getRequestId(),
                value.getMomoOrderId(),
                value.getMomoTransactionId(),
                value.getAmount(),
                value.getResultCode(),
                value.getMessage(),
                value.getCreatedAt()
        );
    }

    private AdminResponses.CallbackData toCallback(MomoCallback value) {
        return new AdminResponses.CallbackData(
                value.getId(),
                value.getCallbackType().name(),
                value.getRequestId(),
                value.getMomoOrderId(),
                value.getMomoTransactionId(),
                value.getResultCode(),
                value.isSignatureValid(),
                value.isProcessed(),
                value.getProcessingMessage(),
                value.getReceivedAt()
        );
    }

    private AdminResponses.CouponData toCoupon(Coupon coupon) {
        return new AdminResponses.CouponData(
                coupon.getId(),
                coupon.getCode(),
                coupon.getName(),
                coupon.getType(),
                coupon.getValue(),
                coupon.getMinimumOrderAmount(),
                coupon.getUsageLimit(),
                coupon.getUsedCount(),
                coupon.getStartsAt(),
                coupon.getEndsAt(),
                coupon.isActive(),
                coupon.getCreatedAt(),
                coupon.getUpdatedAt()
        );
    }

    private AdminResponses.ReviewData toReview(Review review) {
        return new AdminResponses.ReviewData(
                review.getId(),
                review.getProduct().getId(),
                review.getProduct().getName(),
                review.getUser().getId(),
                review.getUser().getFullName(),
                review.getRating(),
                review.getComment(),
                review.getStatus(),
                review.getImages().stream().map(image -> image.getUrl()).toList(),
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }

    private AdminResponses.ConversationData toConversation(Conversation value) {
        return new AdminResponses.ConversationData(
                value.getId(),
                toUser(value.getParticipantOne()),
                toUser(value.getParticipantTwo()),
                value.getAssignedStaff() == null
                        ? null
                        : toUser(value.getAssignedStaff()),
                value.isClosed(),
                value.getClosedAt(),
                value.getLastMessageAt(),
                value.getCreatedAt()
        );
    }

    private AdminResponses.MessageData toMessage(Message value) {
        return new AdminResponses.MessageData(
                value.getId(),
                value.getSender().getId(),
                value.getSender().getFullName(),
                value.getType(),
                value.getContent(),
                value.getReadAt(),
                value.getCreatedAt()
        );
    }

    private AdminResponses.NotificationData toNotification(Notification value) {
        return new AdminResponses.NotificationData(
                value.getId(),
                value.getTitle(),
                value.getContent(),
                value.getTargetRole(),
                value.getCreatedBy().getId(),
                value.getCreatedBy().getFullName(),
                value.getCreatedAt()
        );
    }

    private List<MultipartFile> safeFiles(List<MultipartFile> files) {
        return files == null ? List.of() : files;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private AdminException notFound(String resource) {
        return error(
                AdminErrorCode.RESOURCE_NOT_FOUND,
                resource + " was not found"
        );
    }

    private AdminException error(AdminErrorCode code, String message) {
        return new AdminException(code, message);
    }

    private record SortRequest(String property, Sort.Direction direction) {
    }
}
