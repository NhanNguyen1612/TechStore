package com.techstore.cart.service;

import com.techstore.cart.dto.request.AddCartItemRequest;
import com.techstore.cart.dto.request.RemoveCartItemRequest;
import com.techstore.cart.dto.request.UpdateCartItemRequest;
import com.techstore.cart.dto.response.CartResponse;

public interface CartService {

    CartResponse getCart(Long userId);

    CartResponse addItem(Long userId, AddCartItemRequest request);

    CartResponse updateItem(Long userId, UpdateCartItemRequest request);

    CartResponse removeItem(Long userId, RemoveCartItemRequest request);

    CartResponse clearCart(Long userId);
}
