package com.techstore.brand.exception;

import org.springframework.http.HttpStatus;

public enum BrandErrorCode {
    BRAND_NOT_FOUND(HttpStatus.NOT_FOUND, "Brand not found"),
    BRAND_NAME_EXISTS(HttpStatus.CONFLICT, "Brand name already exists"),
    INVALID_BRAND_QUERY(HttpStatus.BAD_REQUEST, "Brand query parameters are invalid");

    private final HttpStatus status;
    private final String message;

    BrandErrorCode(HttpStatus status, String message) {
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
