package com.techstore.payment.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techstore.auth.entity.Role;
import com.techstore.auth.entity.User;
import com.techstore.auth.repository.UserRepository;
import com.techstore.order.entity.Order;
import com.techstore.order.entity.OrderStatus;
import com.techstore.order.repository.OrderRepository;
import com.techstore.order.service.OrderStatusHistoryService;
import com.techstore.payment.dto.request.MomoCreateApiRequest;
import com.techstore.payment.dto.request.MomoResultRequest;
import com.techstore.payment.dto.response.MomoCreateApiResponse;
import com.techstore.payment.dto.response.MomoIpnResult;
import com.techstore.payment.dto.response.MomoPaymentResponse;
import com.techstore.payment.dto.response.MomoReturnResponse;
import com.techstore.payment.entity.MomoCallback;
import com.techstore.payment.entity.MomoCallbackType;
import com.techstore.payment.entity.Payment;
import com.techstore.payment.entity.PaymentStatus;
import com.techstore.payment.entity.PaymentTransaction;
import com.techstore.payment.entity.PaymentTransactionType;
import com.techstore.payment.exception.PaymentErrorCode;
import com.techstore.payment.exception.PaymentException;
import com.techstore.payment.gateway.MomoGatewayClient;
import com.techstore.payment.gateway.MomoSignatureService;
import com.techstore.payment.repository.MomoCallbackRepository;
import com.techstore.payment.repository.PaymentRepository;
import com.techstore.payment.repository.PaymentTransactionRepository;
import com.techstore.payment.service.PaymentService;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

@Service
public class PaymentServiceImpl implements PaymentService {

    private static final long MIN_MOMO_AMOUNT = 1_000L;
    private static final long MAX_MOMO_AMOUNT = 50_000_000L;
    private static final Set<Integer> CANCELLED_RESULT_CODES = Set.of(1005, 1006);

    private final PaymentRepository paymentRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final MomoCallbackRepository callbackRepository;
    private final OrderRepository orderRepository;
    private final OrderStatusHistoryService orderHistoryService;
    private final UserRepository userRepository;
    private final MomoGatewayClient gatewayClient;
    private final MomoSignatureService signatureService;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final com.techstore.payment.config.MomoProperties properties;

    public PaymentServiceImpl(
            PaymentRepository paymentRepository,
            PaymentTransactionRepository transactionRepository,
            MomoCallbackRepository callbackRepository,
            OrderRepository orderRepository,
            OrderStatusHistoryService orderHistoryService,
            UserRepository userRepository,
            MomoGatewayClient gatewayClient,
            MomoSignatureService signatureService,
            ObjectMapper objectMapper,
            Clock clock,
            com.techstore.payment.config.MomoProperties properties
    ) {
        this.paymentRepository = paymentRepository;
        this.transactionRepository = transactionRepository;
        this.callbackRepository = callbackRepository;
        this.orderRepository = orderRepository;
        this.orderHistoryService = orderHistoryService;
        this.userRepository = userRepository;
        this.gatewayClient = gatewayClient;
        this.signatureService = signatureService;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.properties = properties;
    }

    @Override
    @Transactional(noRollbackFor = PaymentException.class)
    public MomoPaymentResponse createMomoPayment(Long userId, Long orderId) {
        User user = findUser(userId);
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new PaymentException(
                        PaymentErrorCode.ORDER_NOT_FOUND
                ));
        verifyOwner(user, order);
        validatePayable(order);

        Payment payment = paymentRepository.findByOrderIdForUpdate(orderId)
                .orElseGet(() -> paymentRepository.saveAndFlush(
                        new Payment(order, toMomoAmount(order))
                ));
        if (payment.getStatus() == PaymentStatus.PAID) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_ALREADY_PAID);
        }
        Instant now = clock.instant();
        if (payment.hasActiveCheckout(now)) {
            return toResponse(payment);
        }
        expirePaymentIfNecessary(payment, order, now);

        String requestId = generateId("REQ");
        String momoOrderId = generateId("TS" + order.getId());
        String orderInfo = "Thanh toan don hang " + order.getOrderCode();
        MomoCreateApiRequest apiRequest = signatureService.createRequest(
                payment.getAmount(),
                momoOrderId,
                orderInfo,
                requestId
        );
        payment.beginAttempt(
                requestId,
                momoOrderId,
                now.plus(properties.qrExpiration())
        );
        OrderStatus oldOrderStatus = order.getStatus();
        order.markPendingPayment();
        orderHistoryService.record(
                order,
                oldOrderStatus,
                "MoMo payment initiated",
                clock.instant()
        );
        paymentRepository.saveAndFlush(payment);
        orderRepository.saveAndFlush(order);

        String requestPayload = toJson(apiRequest);
        try {
            MomoCreateApiResponse response = gatewayClient.createPayment(apiRequest);
            String responsePayload = toJson(response);
            if (!isMatchingCreateResponse(payment, response)
                    || !signatureService.verifyCreateResponse(response)) {
                failCreate(
                        payment,
                        order,
                        response == null ? null : response.resultCode(),
                        response == null ? "Empty response from MoMo" : response.message(),
                        requestPayload,
                        responsePayload
                );
                throw new PaymentException(PaymentErrorCode.INVALID_MOMO_RESPONSE);
            }

            saveTransaction(
                    payment,
                    PaymentTransactionType.CREATE,
                    null,
                    response.resultCode(),
                    response.message(),
                    requestPayload,
                    responsePayload
            );
            if (response.resultCode() == 0 && response.payUrl() != null) {
                payment.acceptCreateResponse(
                        response.payUrl(),
                        response.deeplink(),
                        response.qrCodeUrl(),
                        response.resultCode(),
                        response.message()
                );
                return toResponse(paymentRepository.saveAndFlush(payment));
            }

            payment.markFailed(response.resultCode(), response.message());
            OrderStatus failedFrom = order.getStatus();
            order.markPaymentFailed();
            orderHistoryService.record(
                    order,
                    failedFrom,
                    valueOrDefault(response.message(), "MoMo payment failed"),
                    clock.instant()
            );
            paymentRepository.saveAndFlush(payment);
            orderRepository.saveAndFlush(order);
            throw new PaymentException(PaymentErrorCode.MOMO_REQUEST_FAILED);
        } catch (RestClientException exception) {
            failCreate(
                    payment,
                    order,
                    null,
                    exception.getMessage(),
                    requestPayload,
                    null
            );
            throw new PaymentException(
                    PaymentErrorCode.MOMO_REQUEST_FAILED,
                    exception
            );
        }
    }

    @Override
    @Transactional
    public MomoIpnResult handleMomoIpn(MomoResultRequest request) {
        String rawPayload = toJson(request);
        boolean signatureValid = signatureService.verifyResult(request);
        MomoCallback callback = callbackRepository.saveAndFlush(
                toCallback(MomoCallbackType.IPN, request, signatureValid, rawPayload)
        );
        if (!signatureValid || !properties.partnerCode().equals(request.partnerCode())) {
            callback.finish(null, false, "Invalid signature or partner code");
            callbackRepository.saveAndFlush(callback);
            return new MomoIpnResult(false, "Invalid signature");
        }

        Payment payment = paymentRepository
                .findByMomoOrderIdForUpdate(request.orderId())
                .orElse(null);
        boolean currentAttempt = payment != null && matchesPayment(payment, request);
        boolean matchedAttempt = currentAttempt;
        if (!currentAttempt) {
            PaymentTransaction previousAttempt = transactionRepository
                    .findTopByMomoOrderIdAndRequestIdOrderByCreatedAtDesc(
                            request.orderId(),
                            request.requestId()
                    )
                    .orElse(null);
            if (previousAttempt != null
                    && request.amount() != null
                    && previousAttempt.getAmount() == request.amount()) {
                payment = paymentRepository.findByOrderIdForUpdate(
                                previousAttempt.getPayment().getOrder().getId()
                        )
                        .orElse(null);
                matchedAttempt = payment != null;
            }
        }
        if (!matchedAttempt) {
            callback.finish(payment, false, "Payment data does not match callback");
            callbackRepository.saveAndFlush(callback);
            return new MomoIpnResult(false, "Payment data mismatch");
        }

        if (currentAttempt
                || (request.resultCode() != null && request.resultCode() == 0)) {
            applyIpn(payment, request);
        }
        saveCallbackTransaction(
                payment,
                PaymentTransactionType.IPN,
                request,
                rawPayload,
                null
        );
        callback.finish(
                payment,
                true,
                currentAttempt ? "Processed" : "Processed previous payment attempt"
        );
        callbackRepository.saveAndFlush(callback);
        return new MomoIpnResult(true, "Processed");
    }

    @Override
    @Transactional
    public MomoReturnResponse handleMomoReturn(MomoResultRequest request) {
        String rawPayload = toJson(request);
        boolean signatureValid = signatureService.verifyResult(request);
        Payment payment = request.orderId() == null
                ? null
                : paymentRepository.findByMomoOrderIdForUpdate(request.orderId())
                        .orElse(null);
        boolean matched = signatureValid
                && properties.partnerCode().equals(request.partnerCode())
                && payment != null
                && matchesPayment(payment, request);

        MomoCallback callback = toCallback(
                MomoCallbackType.RETURN,
                request,
                signatureValid,
                rawPayload
        );
        callback.finish(
                payment,
                matched,
                matched ? "Verified return" : "Return data did not verify"
        );
        callbackRepository.saveAndFlush(callback);
        if (matched) {
            saveTransaction(
                    payment,
                    PaymentTransactionType.RETURN,
                    request.transId(),
                    request.resultCode(),
                    request.message(),
                    rawPayload,
                    null
            );
        }

        return new MomoReturnResponse(
                signatureValid,
                matched,
                payment == null ? null : payment.getOrder().getId(),
                request.orderId(),
                request.resultCode(),
                request.message(),
                payment == null ? null : payment.getStatus()
        );
    }

    @Override
    @Transactional
    public MomoPaymentResponse getPaymentStatus(Long userId, Long orderId) {
        User user = findUser(userId);
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentException(
                        PaymentErrorCode.PAYMENT_NOT_FOUND
                ));
        if (!isManager(user)
                && !payment.getOrder().getUser().getId().equals(userId)) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_ACCESS_DENIED);
        }
        expirePaymentIfNecessary(payment, payment.getOrder(), clock.instant());
        return toResponse(payment);
    }

    private void expirePaymentIfNecessary(
            Payment payment,
            Order order,
            Instant now
    ) {
        if (!payment.expireIfNecessary(now)) {
            return;
        }
        OrderStatus expiredFrom = order.getStatus();
        order.markPaymentFailed();
        orderHistoryService.record(
                order,
                expiredFrom,
                "MoMo QR expired",
                now
        );
        paymentRepository.saveAndFlush(payment);
        orderRepository.saveAndFlush(order);
    }

    private void applyIpn(Payment payment, MomoResultRequest request) {
        Order order = payment.getOrder();
        Integer resultCode = request.resultCode();
        if (resultCode != null && resultCode == 0) {
            payment.markPaid(
                    request.transId(),
                    resultCode,
                    request.message(),
                    clock.instant()
            );
            if (order.getStatus() != OrderStatus.CANCELLED) {
                OrderStatus paidFrom = order.getStatus();
                order.markPaid();
                orderHistoryService.record(
                        order,
                        paidFrom,
                        "MoMo payment completed successfully",
                        clock.instant()
                );
                orderRepository.saveAndFlush(order);
            }
        } else if (resultCode != null && resultCode == 9000) {
            payment.markPending(
                    request.transId(),
                    resultCode,
                    request.message()
            );
        } else if (resultCode != null && CANCELLED_RESULT_CODES.contains(resultCode)) {
            payment.markCancelled(resultCode, request.message());
            OrderStatus cancelledFrom = order.getStatus();
            order.markPaymentFailed();
            orderHistoryService.record(
                    order,
                    cancelledFrom,
                    valueOrDefault(request.message(), "MoMo payment cancelled"),
                    clock.instant()
            );
            orderRepository.saveAndFlush(order);
        } else {
            payment.markFailed(resultCode, request.message());
            OrderStatus failedFrom = order.getStatus();
            order.markPaymentFailed();
            orderHistoryService.record(
                    order,
                    failedFrom,
                    valueOrDefault(request.message(), "MoMo payment failed"),
                    clock.instant()
            );
            orderRepository.saveAndFlush(order);
        }
        paymentRepository.saveAndFlush(payment);
    }

    private void failCreate(
            Payment payment,
            Order order,
            Integer resultCode,
            String message,
            String requestPayload,
            String responsePayload
    ) {
        payment.markFailed(resultCode, message);
        OrderStatus failedFrom = order.getStatus();
        order.markPaymentFailed();
        orderHistoryService.record(
                order,
                failedFrom,
                valueOrDefault(message, "Unable to create MoMo payment"),
                clock.instant()
        );
        paymentRepository.saveAndFlush(payment);
        orderRepository.saveAndFlush(order);
        saveTransaction(
                payment,
                PaymentTransactionType.CREATE,
                null,
                resultCode,
                message,
                requestPayload,
                responsePayload
        );
    }

    private void saveTransaction(
            Payment payment,
            PaymentTransactionType type,
            Long transactionId,
            Integer resultCode,
            String message,
            String requestPayload,
            String responsePayload
    ) {
        transactionRepository.save(new PaymentTransaction(
                payment,
                type,
                payment.getRequestId(),
                payment.getMomoOrderId(),
                transactionId,
                payment.getAmount(),
                resultCode,
                message,
                requestPayload,
                responsePayload
        ));
    }

    private void saveCallbackTransaction(
            Payment payment,
            PaymentTransactionType type,
            MomoResultRequest request,
            String requestPayload,
            String responsePayload
    ) {
        transactionRepository.save(new PaymentTransaction(
                payment,
                type,
                request.requestId(),
                request.orderId(),
                request.transId(),
                payment.getAmount(),
                request.resultCode(),
                request.message(),
                requestPayload,
                responsePayload
        ));
    }

    private MomoCallback toCallback(
            MomoCallbackType type,
            MomoResultRequest request,
            boolean signatureValid,
            String rawPayload
    ) {
        return new MomoCallback(
                type,
                request.partnerCode(),
                request.requestId(),
                request.orderId(),
                request.amount(),
                request.transId(),
                request.resultCode(),
                request.message(),
                signatureValid,
                rawPayload
        );
    }

    private boolean matchesPayment(Payment payment, MomoResultRequest request) {
        return payment.getMomoOrderId().equals(request.orderId())
                && payment.getRequestId().equals(request.requestId())
                && request.amount() != null
                && payment.getAmount() == request.amount();
    }

    private boolean isMatchingCreateResponse(
            Payment payment,
            MomoCreateApiResponse response
    ) {
        return response != null
                && properties.partnerCode().equals(response.partnerCode())
                && payment.getRequestId().equals(response.requestId())
                && payment.getMomoOrderId().equals(response.orderId())
                && response.amount() != null
                && payment.getAmount() == response.amount();
    }

    private void validatePayable(Order order) {
        if (order.getStatus() == OrderStatus.PAID
                || order.getStatus() == OrderStatus.CONFIRMED
                || order.getStatus() == OrderStatus.SHIPPING
                || order.getStatus() == OrderStatus.DELIVERED
                || order.getStatus() == OrderStatus.COMPLETED
                || order.getStatus() == OrderStatus.CANCELLED) {
            throw new PaymentException(PaymentErrorCode.ORDER_NOT_PAYABLE);
        }
    }

    private long toMomoAmount(Order order) {
        try {
            long amount = order.getTotalAmount()
                    .setScale(0, RoundingMode.UNNECESSARY)
                    .longValueExact();
            if (amount < MIN_MOMO_AMOUNT || amount > MAX_MOMO_AMOUNT) {
                throw new PaymentException(
                        PaymentErrorCode.INVALID_PAYMENT_AMOUNT
                );
            }
            return amount;
        } catch (ArithmeticException exception) {
            throw new PaymentException(
                    PaymentErrorCode.INVALID_PAYMENT_AMOUNT,
                    exception
            );
        }
    }

    private void verifyOwner(User user, Order order) {
        if (user.getRole() != Role.ROLE_CUSTOMER
                || !order.getUser().getId().equals(user.getId())) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_ACCESS_DENIED);
        }
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new PaymentException(
                        PaymentErrorCode.PAYMENT_ACCESS_DENIED
                ));
    }

    private boolean isManager(User user) {
        return user.getRole() == Role.ROLE_ADMIN || user.getRole() == Role.ROLE_STAFF;
    }

    private String generateId(String prefix) {
        return prefix + "-" + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .toUpperCase(Locale.ROOT);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            return "{\"serializationError\":true}";
        }
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private MomoPaymentResponse toResponse(Payment payment) {
        return new MomoPaymentResponse(
                payment.getId(),
                payment.getOrder().getId(),
                payment.getOrder().getOrderCode(),
                payment.getStatus(),
                payment.getAmount(),
                payment.getRequestId(),
                payment.getMomoOrderId(),
                payment.getMomoTransactionId(),
                payment.getPayUrl(),
                payment.getDeeplink(),
                payment.getQrCodeUrl(),
                payment.getResultCode(),
                payment.getMessage(),
                payment.getPaidAt(),
                payment.getExpiresAt(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}
