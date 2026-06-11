package com.techstore.wishlist.exception;

import org.springframework.http.HttpStatus;

public enum WishlistErrorCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User not found"),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "Product not found"),
    PRODUCT_ALREADY_WISHLISTED(
            HttpStatus.CONFLICT,
            "Product is already in the wishlist"
    ),
    WISHLIST_ITEM_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "Product is not in the wishlist"
    );

    private final HttpStatus status;
    private final String message;

    WishlistErrorCode(HttpStatus status, String message) {
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
