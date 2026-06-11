package com.techstore.wishlist.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record WishlistItemResponse(
        Long id,
        Long productId,
        String productName,
        String slug,
        String sku,
        BigDecimal price,
        int stockQuantity,
        String thumbnailUrl,
        Long categoryId,
        String categoryName,
        Long brandId,
        String brandName,
        boolean wishlisted,
        Instant addedAt
) {
}
