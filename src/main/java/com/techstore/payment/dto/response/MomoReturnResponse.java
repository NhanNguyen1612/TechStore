package com.techstore.payment.dto.response;

import com.techstore.payment.entity.PaymentStatus;

public record MomoReturnResponse(
        boolean signatureValid,
        boolean matched,
        Long orderId,
        String momoOrderId,
        Integer resultCode,
        String message,
        PaymentStatus paymentStatus
) {
}
