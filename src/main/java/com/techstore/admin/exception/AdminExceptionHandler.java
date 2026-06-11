package com.techstore.admin.exception;

import com.techstore.auth.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.techstore.admin")
public class AdminExceptionHandler {

    @ExceptionHandler(AdminException.class)
    public ResponseEntity<ApiResponse<Void>> handle(AdminException exception) {
        AdminErrorCode error = exception.getErrorCode();
        return ResponseEntity
                .status(error.getStatus())
                .body(ApiResponse.error(error.getCode(), exception.getMessage(), null));
    }
}
