package com.techstore.review.dto.response;

import com.techstore.review.entity.ReviewStatus;
import java.time.Instant;
import java.util.List;

public record ReviewResponse(
        Long id,
        Long productId,
        Long userId,
        String userName,
        int rating,
        String comment,
        boolean verifiedPurchase,
        ReviewStatus status,
        List<ReviewImageResponse> images,
        Instant approvedAt,
        Long approvedBy,
        Instant createdAt,
        Instant updatedAt
) {
}
