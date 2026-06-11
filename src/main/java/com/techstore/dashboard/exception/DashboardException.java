package com.techstore.dashboard.exception;

public class DashboardException extends RuntimeException {

    private final DashboardErrorCode errorCode;

    public DashboardException(DashboardErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public DashboardErrorCode getErrorCode() {
        return errorCode;
    }
}
