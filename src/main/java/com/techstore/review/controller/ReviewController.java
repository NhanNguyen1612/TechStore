package com.techstore.review.controller;

import com.techstore.auth.dto.response.ApiResponse;
import com.techstore.auth.security.AuthUserPrincipal;
import com.techstore.review.dto.request.CreateReviewRequest;
import com.techstore.review.dto.request.UpdateReviewRequest;
import com.techstore.review.dto.response.ProductReviewsResponse;
import com.techstore.review.dto.response.ReviewResponse;
import com.techstore.review.service.ReviewService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<ReviewResponse>> createReviewJson(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @Valid @RequestBody CreateReviewRequest request
    ) {
        return created(reviewService.createReview(
                principal.getId(),
                request,
                List.of()
        ));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<ReviewResponse>> createReviewMultipart(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @Valid @RequestPart("request") CreateReviewRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) {
        return created(reviewService.createReview(
                principal.getId(),
                request,
                images
        ));
    }

    @GetMapping("/product/{id}")
    public ApiResponse<ProductReviewsResponse> getProductReviews(
            @PathVariable Long id
    ) {
        return ApiResponse.success(
                "Product reviews retrieved",
                reviewService.getProductReviews(id)
        );
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<ReviewResponse> updateReviewJson(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateReviewRequest request
    ) {
        return updated(reviewService.updateReview(
                principal.getId(),
                id,
                request,
                List.of()
        ));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ReviewResponse> updateReviewMultipart(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestPart("request") UpdateReviewRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) {
        return updated(reviewService.updateReview(
                principal.getId(),
                id,
                request,
                images
        ));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteReview(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @PathVariable Long id
    ) {
        reviewService.deleteReview(principal.getId(), id);
        return ApiResponse.success("Review deleted");
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ReviewResponse> approveReview(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @PathVariable Long id
    ) {
        return ApiResponse.success(
                "Review approved",
                reviewService.approveReview(principal.getId(), id)
        );
    }

    private ResponseEntity<ApiResponse<ReviewResponse>> created(
            ReviewResponse review
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Review created", review));
    }

    private ApiResponse<ReviewResponse> updated(ReviewResponse review) {
        return ApiResponse.success("Review updated", review);
    }
}
