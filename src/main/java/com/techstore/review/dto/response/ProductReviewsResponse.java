package com.techstore.review.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record ProductReviewsResponse(
        Long productId,
        long totalReviews,
        BigDecimal averageRating,
        List<ReviewResponse> reviews
) {
}
