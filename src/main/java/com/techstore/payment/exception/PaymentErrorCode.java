package com.techstore.payment.exception;

import org.springframework.http.HttpStatus;

public enum PaymentErrorCode {
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "Order not found"),
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "Payment not found"),
    PAYMENT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "Access to this payment is denied"),
    ORDER_NOT_PAYABLE(HttpStatus.CONFLICT, "Order cannot be paid in its current status"),
    PAYMENT_ALREADY_PAID(HttpStatus.CONFLICT, "Order has already been paid"),
    INVALID_PAYMENT_AMOUNT(HttpStatus.BAD_REQUEST, "Payment amount must be valid VND"),
    MOMO_REQUEST_FAILED(HttpStatus.BAD_GATEWAY, "Unable to create MoMo payment"),
    INVALID_MOMO_RESPONSE(HttpStatus.BAD_GATEWAY, "MoMo response signature is invalid"),
    INVALID_MOMO_CALLBACK(HttpStatus.BAD_REQUEST, "MoMo callback is invalid");

    private final HttpStatus status;
    private final String message;

    PaymentErrorCode(HttpStatus status, String message) {
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
