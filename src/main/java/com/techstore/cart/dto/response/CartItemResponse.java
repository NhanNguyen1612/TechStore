package com.techstore.cart.dto.response;

import java.math.BigDecimal;

public record CartItemResponse(
        Long id,
        Long productId,
        String productName,
        String sku,
        String thumbnailUrl,
        BigDecimal unitPrice,
        int quantity,
        int availableStock,
        boolean available,
        BigDecimal subtotal
) {
}
