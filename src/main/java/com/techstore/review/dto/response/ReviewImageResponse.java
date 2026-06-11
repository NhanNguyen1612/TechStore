package com.techstore.review.dto.response;

public record ReviewImageResponse(
        Long id,
        String url,
        int sortOrder
) {
}
