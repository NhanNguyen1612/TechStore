package com.techstore.wishlist.dto.response;

public record WishlistStatusResponse(
        Long productId,
        boolean wishlisted
) {
}
