package com.techstore.payment.dto.response;

public record MomoIpnResult(
        boolean accepted,
        String message
) {
}
