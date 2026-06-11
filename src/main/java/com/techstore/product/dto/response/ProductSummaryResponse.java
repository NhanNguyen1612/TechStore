package com.techstore.product.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductSummaryResponse(
        Long id,
        String name,
        String slug,
        String sku,
        BigDecimal price,
        int stockQuantity,
        long soldCount,
        String thumbnailUrl,
        Long categoryId,
        String categoryName,
        Long brandId,
        String brandName,
        Instant createdAt
) {
}
