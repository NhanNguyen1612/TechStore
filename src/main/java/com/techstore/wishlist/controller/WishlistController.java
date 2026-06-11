package com.techstore.wishlist.controller;

import com.techstore.auth.dto.response.ApiResponse;
import com.techstore.auth.security.AuthUserPrincipal;
import com.techstore.wishlist.dto.response.WishlistItemResponse;
import com.techstore.wishlist.dto.response.WishlistResponse;
import com.techstore.wishlist.dto.response.WishlistStatusResponse;
import com.techstore.wishlist.service.WishlistService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wishlist")
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @GetMapping
    public ApiResponse<WishlistResponse> getWishlist(
            @AuthenticationPrincipal AuthUserPrincipal principal
    ) {
        return ApiResponse.success(
                "Wishlist retrieved",
                wishlistService.getWishlist(principal.getId())
        );
    }

    @PostMapping("/{productId}")
    public ApiResponse<WishlistItemResponse> addProduct(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @PathVariable Long productId
    ) {
        return ApiResponse.success(
                "Product added to wishlist",
                wishlistService.addProduct(principal.getId(), productId)
        );
    }

    @DeleteMapping("/{productId}")
    public ApiResponse<WishlistStatusResponse> removeProduct(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @PathVariable Long productId
    ) {
        return ApiResponse.success(
                "Product removed from wishlist",
                wishlistService.removeProduct(principal.getId(), productId)
        );
    }
}
