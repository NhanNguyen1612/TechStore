package com.techstore.user.exception;

public class UserModuleException extends RuntimeException {

    private final UserErrorCode errorCode;

    public UserModuleException(UserErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public UserModuleException(UserErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    public UserErrorCode getErrorCode() {
        return errorCode;
    }
}
