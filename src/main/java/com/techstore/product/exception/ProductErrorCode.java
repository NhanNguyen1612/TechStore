package com.techstore.product.exception;

import org.springframework.http.HttpStatus;

public enum ProductErrorCode {
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "Product not found"),
    CATEGORY_NOT_FOUND(HttpStatus.BAD_REQUEST, "Category not found"),
    BRAND_NOT_FOUND(HttpStatus.BAD_REQUEST, "Brand not found"),
    SKU_ALREADY_EXISTS(HttpStatus.CONFLICT, "Product SKU already exists"),
    INVALID_PRODUCT_QUERY(HttpStatus.BAD_REQUEST, "Product query parameters are invalid"),
    INVALID_PRICE_RANGE(HttpStatus.BAD_REQUEST, "Minimum price must not exceed maximum price"),
    TOO_MANY_IMAGES(HttpStatus.BAD_REQUEST, "Too many product images"),
    IMAGE_REQUIRED(HttpStatus.BAD_REQUEST, "Product image file is empty"),
    INVALID_IMAGE_TYPE(HttpStatus.BAD_REQUEST, "Images must be JPEG, PNG, or WebP"),
    IMAGE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "A product image is too large"),
    IMAGE_UPLOAD_FAILED(HttpStatus.BAD_GATEWAY, "Product image upload failed");

    private final HttpStatus status;
    private final String message;

    ProductErrorCode(HttpStatus status, String message) {
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
