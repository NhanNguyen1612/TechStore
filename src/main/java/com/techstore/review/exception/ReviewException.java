package com.techstore.review.exception;

public class ReviewException extends RuntimeException {

    private final ReviewErrorCode errorCode;

    public ReviewException(ReviewErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ReviewException(ReviewErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    public ReviewErrorCode getErrorCode() {
        return errorCode;
    }
}
