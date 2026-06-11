package com.techstore.product.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ProductDetailResponse(
        Long id,
        String name,
        String slug,
        String sku,
        String description,
        BigDecimal price,
        int stockQuantity,
        long soldCount,
        Long categoryId,
        String categoryName,
        Long brandId,
        String brandName,
        List<ProductImageResponse> images,
        Instant createdAt,
        Instant updatedAt,
        Long createdBy,
        Long updatedBy
) {
}
