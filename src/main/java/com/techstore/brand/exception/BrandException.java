package com.techstore.brand.exception;

public class BrandException extends RuntimeException {

    private final BrandErrorCode errorCode;

    public BrandException(BrandErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BrandErrorCode getErrorCode() {
        return errorCode;
    }
}
