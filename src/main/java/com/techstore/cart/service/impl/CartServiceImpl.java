package com.techstore.cart.service.impl;

import com.techstore.auth.entity.User;
import com.techstore.auth.repository.UserRepository;
import com.techstore.cart.dto.request.AddCartItemRequest;
import com.techstore.cart.dto.request.RemoveCartItemRequest;
import com.techstore.cart.dto.request.UpdateCartItemRequest;
import com.techstore.cart.dto.response.CartItemResponse;
import com.techstore.cart.dto.response.CartResponse;
import com.techstore.cart.entity.Cart;
import com.techstore.cart.entity.CartItem;
import com.techstore.cart.exception.CartErrorCode;
import com.techstore.cart.exception.CartException;
import com.techstore.cart.repository.CartRepository;
import com.techstore.cart.service.CartService;
import com.techstore.product.entity.Product;
import com.techstore.product.repository.ProductRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public CartServiceImpl(
            CartRepository cartRepository,
            UserRepository userRepository,
            ProductRepository productRepository
    ) {
        this.cartRepository = cartRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .map(this::toResponse)
                .orElseGet(this::emptyCart);
    }

    @Override
    @Transactional
    public CartResponse addItem(Long userId, AddCartItemRequest request) {
        Product product = findActiveProduct(request.productId());
        Cart cart = findOrCreateCart(userId);
        CartItem item = cart.findItem(product.getId()).orElse(null);
        long requestedQuantity = (long) request.quantity()
                + (item == null ? 0 : item.getQuantity());

        validateStock(product, requestedQuantity);
        int quantity = Math.toIntExact(requestedQuantity);
        if (item == null) {
            cart.addItem(product, quantity);
        } else {
            item.updateQuantity(quantity);
        }

        return toResponse(cartRepository.saveAndFlush(cart));
    }

    @Override
    @Transactional
    public CartResponse updateItem(Long userId, UpdateCartItemRequest request) {
        Cart cart = findCart(userId);
        CartItem item = findItem(cart, request.productId());
        Product product = item.getProduct();

        if (product.isDeleted()) {
            throw new CartException(CartErrorCode.PRODUCT_NOT_FOUND);
        }
        validateStock(product, request.quantity());
        item.updateQuantity(request.quantity());
        return toResponse(cartRepository.saveAndFlush(cart));
    }

    @Override
    @Transactional
    public CartResponse removeItem(Long userId, RemoveCartItemRequest request) {
        Cart cart = findCart(userId);
        CartItem item = findItem(cart, request.productId());
        cart.removeItem(item);
        return toResponse(cartRepository.saveAndFlush(cart));
    }

    @Override
    @Transactional
    public CartResponse clearCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId).orElse(null);
        if (cart == null) {
            return emptyCart();
        }
        cart.clear();
        return toResponse(cartRepository.saveAndFlush(cart));
    }

    private Cart findOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> new Cart(findUser(userId)));
    }

    private Cart findCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseThrow(() -> new CartException(CartErrorCode.CART_ITEM_NOT_FOUND));
    }

    private CartItem findItem(Cart cart, Long productId) {
        return cart.findItem(productId)
                .orElseThrow(() -> new CartException(CartErrorCode.CART_ITEM_NOT_FOUND));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CartException(CartErrorCode.USER_NOT_FOUND));
    }

    private Product findActiveProduct(Long productId) {
        return productRepository.findByIdAndDeletedFalse(productId)
                .orElseThrow(() -> new CartException(CartErrorCode.PRODUCT_NOT_FOUND));
    }

    private void validateStock(Product product, long quantity) {
        if (quantity > product.getStockQuantity()) {
            throw new CartException(CartErrorCode.INSUFFICIENT_STOCK);
        }
    }

    private CartResponse toResponse(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream()
                .map(this::toItemResponse)
                .toList();
        int totalQuantity = items.stream()
                .mapToInt(CartItemResponse::quantity)
                .sum();
        BigDecimal totalAmount = items.stream()
                .map(CartItemResponse::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        return new CartResponse(cart.getId(), items, totalQuantity, totalAmount);
    }

    private CartItemResponse toItemResponse(CartItem item) {
        Product product = item.getProduct();
        BigDecimal subtotal = product.getPrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()))
                .setScale(2, RoundingMode.HALF_UP);
        boolean available = !product.isDeleted()
                && item.getQuantity() <= product.getStockQuantity();
        return new CartItemResponse(
                item.getId(),
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getThumbnailUrl(),
                product.getPrice(),
                item.getQuantity(),
                product.getStockQuantity(),
                available,
                subtotal
        );
    }

    private CartResponse emptyCart() {
        return new CartResponse(
                null,
                List.of(),
                0,
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
        );
    }
}
