package com.techstore.admin.exception;

import org.springframework.http.HttpStatus;

public enum AdminErrorCode {
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "ADMIN_RESOURCE_NOT_FOUND"),
    EMAIL_EXISTS(HttpStatus.CONFLICT, "ADMIN_EMAIL_EXISTS"),
    INVALID_ORDER_TRANSITION(HttpStatus.CONFLICT, "INVALID_ORDER_TRANSITION"),
    TAXONOMY_IN_USE(HttpStatus.CONFLICT, "TAXONOMY_IN_USE"),
    INVALID_COUPON(HttpStatus.BAD_REQUEST, "INVALID_COUPON"),
    COUPON_EXISTS(HttpStatus.CONFLICT, "COUPON_EXISTS"),
    INVALID_STAFF(HttpStatus.BAD_REQUEST, "INVALID_STAFF"),
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE");

    private final HttpStatus status;
    private final String code;

    AdminErrorCode(HttpStatus status, String code) {
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
