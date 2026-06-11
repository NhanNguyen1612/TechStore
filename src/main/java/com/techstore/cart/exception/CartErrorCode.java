package com.techstore.cart.exception;

import org.springframework.http.HttpStatus;

public enum CartErrorCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User not found"),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "Product not found"),
    CART_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "Product is not in the cart"),
    INSUFFICIENT_STOCK(HttpStatus.CONFLICT, "Requested quantity exceeds available stock");

    private final HttpStatus status;
    private final String message;

    CartErrorCode(HttpStatus status, String message) {
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
