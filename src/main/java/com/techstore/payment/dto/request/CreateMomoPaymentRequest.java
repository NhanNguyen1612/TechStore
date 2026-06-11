package com.techstore.payment.dto.request;

import jakarta.validation.constraints.NotNull;

public record CreateMomoPaymentRequest(
        @NotNull(message = "Order ID is required")
        Long orderId
) {
}
