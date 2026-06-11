package com.techstore.order.dto.response;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long id,
        Long productId,
        String productName,
        String sku,
        String thumbnailUrl,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal subtotal
) {
}
