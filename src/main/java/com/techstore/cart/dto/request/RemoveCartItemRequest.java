package com.techstore.cart.dto.request;

import jakarta.validation.constraints.NotNull;

public record RemoveCartItemRequest(
        @NotNull(message = "Product ID is required")
        Long productId
) {
}
