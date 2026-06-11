package com.techstore.user.exception;

import org.springframework.http.HttpStatus;

public enum UserErrorCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User not found"),
    AVATAR_REQUIRED(HttpStatus.BAD_REQUEST, "Avatar file is required"),
    INVALID_AVATAR_TYPE(HttpStatus.BAD_REQUEST, "Avatar must be a JPEG, PNG, or WebP image"),
    AVATAR_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "Avatar file is too large"),
    AVATAR_UPLOAD_FAILED(HttpStatus.BAD_GATEWAY, "Avatar upload failed"),
    LAST_ADMIN_PROTECTED(HttpStatus.CONFLICT, "The last active administrator cannot be disabled or reassigned");

    private final HttpStatus status;
    private final String message;

    UserErrorCode(HttpStatus status, String message) {
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
