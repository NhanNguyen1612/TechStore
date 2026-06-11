package com.techstore.brand.dto.response;

import java.time.Instant;

public record BrandResponse(
        Long id,
        String name,
        String slug,
        String description,
        Instant createdAt,
        Instant updatedAt,
        Long createdBy,
        Long updatedBy
) {
}
