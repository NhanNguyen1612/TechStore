package com.techstore.category.exception;

import com.techstore.auth.dto.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class CategoryExceptionHandler {

    @ExceptionHandler(CategoryException.class)
    public ResponseEntity<ApiResponse<Void>> handleCategoryException(
            CategoryException exception
    ) {
        CategoryErrorCode error = exception.getErrorCode();
        return ResponseEntity
                .status(error.getStatus())
                .body(ApiResponse.error(error.name(), error.getMessage(), null));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation() {
        CategoryErrorCode error = CategoryErrorCode.INVALID_CATEGORY_QUERY;
        return ResponseEntity
                .status(error.getStatus())
                .body(ApiResponse.error(error.name(), error.getMessage(), null));
    }
}
