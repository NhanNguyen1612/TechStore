package com.techstore.cart.exception;

public class CartException extends RuntimeException {

    private final CartErrorCode errorCode;

    public CartException(CartErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public CartErrorCode getErrorCode() {
        return errorCode;
    }
}
