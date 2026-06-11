package com.techstore.admin.exception;

public class AdminException extends RuntimeException {

    private final AdminErrorCode errorCode;

    public AdminException(AdminErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public AdminErrorCode getErrorCode() {
        return errorCode;
    }
}
