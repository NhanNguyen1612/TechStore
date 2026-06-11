package com.techstore.chat.exception;

public class ChatException extends RuntimeException {

    private final ChatErrorCode errorCode;

    public ChatException(ChatErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ChatException(ChatErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    public ChatErrorCode getErrorCode() {
        return errorCode;
    }
}
