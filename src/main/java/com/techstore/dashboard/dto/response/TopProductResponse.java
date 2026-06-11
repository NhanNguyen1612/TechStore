package com.techstore.dashboard.dto.response;

import java.math.BigDecimal;

public record TopProductResponse(
        Long productId,
        String productName,
        String sku,
        String thumbnailUrl,
        long quantitySold,
        BigDecimal revenue
) {
}
