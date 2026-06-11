package com.techstore.chat.exception;

import com.techstore.auth.dto.response.ApiResponse;
import com.techstore.auth.exception.ErrorCode;
import jakarta.validation.ConstraintViolationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class ChatExceptionHandler {

    @ExceptionHandler(ChatException.class)
    public ResponseEntity<ApiResponse<Void>> handleChatException(
            ChatException exception
    ) {
        ChatErrorCode error = exception.getErrorCode();
        return ResponseEntity
                .status(error.getStatus())
                .body(ApiResponse.error(error.name(), error.getMessage(), null));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation() {
        ErrorCode error = ErrorCode.VALIDATION_ERROR;
        return ResponseEntity
                .status(error.getStatus())
                .body(ApiResponse.error(error.name(), error.getMessage(), null));
    }
}
