package com.techstore.brand.exception;

import com.techstore.auth.dto.response.ApiResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class BrandExceptionHandler {

    @ExceptionHandler(BrandException.class)
    public ResponseEntity<ApiResponse<Void>> handleBrandException(
            BrandException exception
    ) {
        BrandErrorCode error = exception.getErrorCode();
        return ResponseEntity
                .status(error.getStatus())
                .body(ApiResponse.error(error.name(), error.getMessage(), null));
    }
}
