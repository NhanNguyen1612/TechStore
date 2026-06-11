package com.techstore.payment.exception;

public class PaymentException extends RuntimeException {

    private final PaymentErrorCode errorCode;

    public PaymentException(PaymentErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public PaymentException(PaymentErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    public PaymentErrorCode getErrorCode() {
        return errorCode;
    }
}
