package com.techstore.order.exception;

import org.springframework.http.HttpStatus;

public enum OrderErrorCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User not found"),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "Order not found"),
    CART_EMPTY(HttpStatus.BAD_REQUEST, "Cart is empty"),
    PRODUCT_UNAVAILABLE(HttpStatus.CONFLICT, "A product is no longer available"),
    INSUFFICIENT_STOCK(HttpStatus.CONFLICT, "A product has insufficient stock"),
    INVALID_STATUS_TRANSITION(HttpStatus.CONFLICT, "Order status transition is invalid"),
    ORDER_ACCESS_DENIED(HttpStatus.FORBIDDEN, "Access to this order is denied");

    private final HttpStatus status;
    private final String message;

    OrderErrorCode(HttpStatus status, String message) {
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
