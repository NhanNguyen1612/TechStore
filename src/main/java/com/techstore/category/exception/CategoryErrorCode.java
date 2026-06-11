package com.techstore.category.exception;

import org.springframework.http.HttpStatus;

public enum CategoryErrorCode {
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "Category not found"),
    CATEGORY_NAME_EXISTS(HttpStatus.CONFLICT, "Category name already exists"),
    INVALID_CATEGORY_QUERY(HttpStatus.BAD_REQUEST, "Category query parameters are invalid");

    private final HttpStatus status;
    private final String message;

    CategoryErrorCode(HttpStatus status, String message) {
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
