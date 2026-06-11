package com.techstore.payment.dto.response;

public record MomoCreateApiResponse(
        String partnerCode,
        String requestId,
        String orderId,
        Long amount,
        Long responseTime,
        String message,
        Integer resultCode,
        String payUrl,
        String deeplink,
        String qrCodeUrl,
        String signature
) {
}
