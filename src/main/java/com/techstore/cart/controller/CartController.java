package com.techstore.cart.controller;

import com.techstore.auth.dto.response.ApiResponse;
import com.techstore.auth.security.AuthUserPrincipal;
import com.techstore.cart.dto.request.AddCartItemRequest;
import com.techstore.cart.dto.request.RemoveCartItemRequest;
import com.techstore.cart.dto.request.UpdateCartItemRequest;
import com.techstore.cart.dto.response.CartResponse;
import com.techstore.cart.service.CartService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
@PreAuthorize("hasRole('CUSTOMER')")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ApiResponse<CartResponse> getCart(
            @AuthenticationPrincipal AuthUserPrincipal principal
    ) {
        return ApiResponse.success(
                "Cart retrieved",
                cartService.getCart(principal.getId())
        );
    }

    @PostMapping("/add")
    public ApiResponse<CartResponse> addItem(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @Valid @RequestBody AddCartItemRequest request
    ) {
        return ApiResponse.success(
                "Product added to cart",
                cartService.addItem(principal.getId(), request)
        );
    }

    @PutMapping("/update")
    public ApiResponse<CartResponse> updateItem(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @Valid @RequestBody UpdateCartItemRequest request
    ) {
        return ApiResponse.success(
                "Cart item updated",
                cartService.updateItem(principal.getId(), request)
        );
    }

    @DeleteMapping("/remove")
    public ApiResponse<CartResponse> removeItem(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @Valid @RequestBody RemoveCartItemRequest request
    ) {
        return ApiResponse.success(
                "Product removed from cart",
                cartService.removeItem(principal.getId(), request)
        );
    }

    @DeleteMapping("/clear")
    public ApiResponse<CartResponse> clearCart(
            @AuthenticationPrincipal AuthUserPrincipal principal
    ) {
        return ApiResponse.success(
                "Cart cleared",
                cartService.clearCart(principal.getId())
        );
    }
}
