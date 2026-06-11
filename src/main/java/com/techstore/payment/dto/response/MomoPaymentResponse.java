package com.techstore.payment.dto.response;

import com.techstore.payment.entity.PaymentStatus;
import java.time.Instant;

public record MomoPaymentResponse(
        Long paymentId,
        Long orderId,
        String orderCode,
        PaymentStatus status,
        long amount,
        String requestId,
        String momoOrderId,
        Long momoTransactionId,
        String payUrl,
        String deeplink,
        String qrCodeUrl,
        Integer resultCode,
        String message,
        Instant paidAt,
        Instant expiresAt,
        Instant createdAt,
        Instant updatedAt
) {
}
