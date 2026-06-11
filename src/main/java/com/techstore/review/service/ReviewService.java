package com.techstore.review.service;

import com.techstore.review.dto.request.CreateReviewRequest;
import com.techstore.review.dto.request.UpdateReviewRequest;
import com.techstore.review.dto.response.ProductReviewsResponse;
import com.techstore.review.dto.response.ReviewResponse;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface ReviewService {

    ReviewResponse createReview(
            Long userId,
            CreateReviewRequest request,
            List<MultipartFile> images
    );

    ProductReviewsResponse getProductReviews(Long productId);

    ReviewResponse updateReview(
            Long userId,
            Long reviewId,
            UpdateReviewRequest request,
            List<MultipartFile> images
    );

    void deleteReview(Long userId, Long reviewId);

    ReviewResponse approveReview(Long adminId, Long reviewId);
}
