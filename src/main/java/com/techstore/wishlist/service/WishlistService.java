package com.techstore.wishlist.service;

import com.techstore.wishlist.dto.response.WishlistItemResponse;
import com.techstore.wishlist.dto.response.WishlistResponse;
import com.techstore.wishlist.dto.response.WishlistStatusResponse;

public interface WishlistService {

    WishlistResponse getWishlist(Long userId);

    WishlistItemResponse addProduct(Long userId, Long productId);

    WishlistStatusResponse removeProduct(Long userId, Long productId);
}
