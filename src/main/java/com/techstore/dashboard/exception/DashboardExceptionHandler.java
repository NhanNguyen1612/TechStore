package com.techstore.dashboard.exception;

import com.techstore.auth.dto.response.ApiResponse;
import com.techstore.dashboard.controller.DashboardController;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = DashboardController.class)
public class DashboardExceptionHandler {

    @ExceptionHandler(DashboardException.class)
    public ResponseEntity<ApiResponse<Void>> handleDashboardException(
            DashboardException exception
    ) {
        DashboardErrorCode error = exception.getErrorCode();
        return ResponseEntity
                .status(error.getStatus())
                .body(ApiResponse.error(error.name(), error.getMessage(), null));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch() {
        DashboardErrorCode error = DashboardErrorCode.INVALID_DASHBOARD_QUERY;
        return ResponseEntity
                .status(error.getStatus())
                .body(ApiResponse.error(error.name(), error.getMessage(), null));
    }
}
