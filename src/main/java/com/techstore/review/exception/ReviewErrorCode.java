package com.techstore.review.exception;

import org.springframework.http.HttpStatus;

public enum ReviewErrorCode {
    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "Review not found"),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "Product not found"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User not found"),
    PRODUCT_NOT_PURCHASED(
            HttpStatus.FORBIDDEN,
            "Only customers who purchased this product can review it"
    ),
    REVIEW_ALREADY_EXISTS(
            HttpStatus.CONFLICT,
            "This product has already been reviewed by the customer"
    ),
    REVIEW_ACCESS_DENIED(HttpStatus.FORBIDDEN, "Access to this review is denied"),
    TOO_MANY_IMAGES(HttpStatus.BAD_REQUEST, "Too many review images"),
    IMAGE_REQUIRED(HttpStatus.BAD_REQUEST, "Review image file is empty"),
    INVALID_IMAGE_TYPE(HttpStatus.BAD_REQUEST, "Images must be JPEG, PNG, or WebP"),
    IMAGE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "A review image is too large"),
    IMAGE_UPLOAD_FAILED(HttpStatus.BAD_GATEWAY, "Review image upload failed");

    private final HttpStatus status;
    private final String message;

    ReviewErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
