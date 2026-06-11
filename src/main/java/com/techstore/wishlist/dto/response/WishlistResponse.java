package com.techstore.wishlist.dto.response;

import java.util.List;

public record WishlistResponse(
        List<WishlistItemResponse> items,
        int totalItems
) {
}
