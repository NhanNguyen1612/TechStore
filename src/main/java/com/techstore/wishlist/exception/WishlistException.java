package com.techstore.wishlist.exception;

public class WishlistException extends RuntimeException {

    private final WishlistErrorCode errorCode;

    public WishlistException(WishlistErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public WishlistErrorCode getErrorCode() {
        return errorCode;
    }
}
