package com.techstore.order.exception;

public class OrderException extends RuntimeException {

    private final OrderErrorCode errorCode;

    public OrderException(OrderErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public OrderErrorCode getErrorCode() {
        return errorCode;
    }
}
