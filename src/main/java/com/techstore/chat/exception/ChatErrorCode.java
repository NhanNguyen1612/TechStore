package com.techstore.chat.exception;

import org.springframework.http.HttpStatus;

public enum ChatErrorCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User not found"),
    CONVERSATION_NOT_FOUND(HttpStatus.NOT_FOUND, "Conversation not found"),
    CONVERSATION_ACCESS_DENIED(
            HttpStatus.FORBIDDEN,
            "You are not a participant in this conversation"
    ),
    INVALID_CONVERSATION_PARTICIPANTS(
            HttpStatus.BAD_REQUEST,
            "A user cannot start a conversation with themselves"
    ),
    INVALID_MESSAGE(HttpStatus.BAD_REQUEST, "Invalid message"),
    IMAGE_REQUIRED(HttpStatus.BAD_REQUEST, "Chat image is required"),
    INVALID_IMAGE_TYPE(
            HttpStatus.UNSUPPORTED_MEDIA_TYPE,
            "Only JPEG, PNG and WebP images are supported"
    ),
    IMAGE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "Chat image is too large"),
    IMAGE_UPLOAD_FAILED(HttpStatus.BAD_GATEWAY, "Unable to upload chat image");

    private final HttpStatus status;
    private final String message;

    ChatErrorCode(HttpStatus status, String message) {
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
