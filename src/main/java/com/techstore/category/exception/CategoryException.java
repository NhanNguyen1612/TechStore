package com.techstore.category.exception;

public class CategoryException extends RuntimeException {

    private final CategoryErrorCode errorCode;

    public CategoryException(CategoryErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public CategoryErrorCode getErrorCode() {
        return errorCode;
    }
}
