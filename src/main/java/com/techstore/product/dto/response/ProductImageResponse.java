package com.techstore.product.dto.response;

public record ProductImageResponse(
        Long id,
        String url,
        int sortOrder,
        boolean primary
) {
}
