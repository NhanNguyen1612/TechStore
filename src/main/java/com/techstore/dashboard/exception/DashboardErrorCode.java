package com.techstore.dashboard.exception;

import org.springframework.http.HttpStatus;

public enum DashboardErrorCode {
    INVALID_DATE_RANGE(
            HttpStatus.BAD_REQUEST,
            "The from date must not be after the to date"
    ),
    INVALID_DASHBOARD_QUERY(
            HttpStatus.BAD_REQUEST,
            "Dashboard query parameters are invalid"
    );

    private final HttpStatus status;
    private final String message;

    DashboardErrorCode(HttpStatus status, String message) {
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
