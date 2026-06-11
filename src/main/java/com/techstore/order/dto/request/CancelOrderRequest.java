package com.techstore.order.dto.request;

import jakarta.validation.constraints.Size;

public record CancelOrderRequest(
        @Size(max = 1000, message = "Cancellation reason must not exceed 1000 characters")
        String reason
) {
}
