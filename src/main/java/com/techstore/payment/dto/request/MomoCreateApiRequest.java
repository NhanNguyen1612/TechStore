package com.techstore.payment.dto.request;

public record MomoCreateApiRequest(
        String partnerCode,
        String requestType,
        String ipnUrl,
        String redirectUrl,
        String orderId,
        long amount,
        String orderInfo,
        String requestId,
        String extraData,
        String signature,
        String lang
) {
}
