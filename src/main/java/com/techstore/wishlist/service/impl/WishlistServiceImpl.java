package com.techstore.wishlist.service.impl;

import com.techstore.auth.entity.User;
import com.techstore.auth.repository.UserRepository;
import com.techstore.product.entity.Product;
import com.techstore.product.repository.ProductRepository;
import com.techstore.wishlist.dto.response.WishlistItemResponse;
import com.techstore.wishlist.dto.response.WishlistResponse;
import com.techstore.wishlist.dto.response.WishlistStatusResponse;
import com.techstore.wishlist.entity.Wishlist;
import com.techstore.wishlist.exception.WishlistErrorCode;
import com.techstore.wishlist.exception.WishlistException;
import com.techstore.wishlist.repository.WishlistRepository;
import com.techstore.wishlist.service.WishlistService;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public WishlistServiceImpl(
            WishlistRepository wishlistRepository,
            UserRepository userRepository,
            ProductRepository productRepository
    ) {
        this.wishlistRepository = wishlistRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public WishlistResponse getWishlist(Long userId) {
        List<WishlistItemResponse> items = wishlistRepository
                .findAllByUserIdAndProductDeletedFalseOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
        return new WishlistResponse(items, items.size());
    }

    @Override
    @Transactional
    public WishlistItemResponse addProduct(Long userId, Long productId) {
        if (wishlistRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new WishlistException(
                    WishlistErrorCode.PRODUCT_ALREADY_WISHLISTED
            );
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new WishlistException(WishlistErrorCode.USER_NOT_FOUND)
                );
        Product product = productRepository.findByIdAndDeletedFalse(productId)
                .orElseThrow(() ->
                        new WishlistException(WishlistErrorCode.PRODUCT_NOT_FOUND)
                );

        try {
            return toResponse(
                    wishlistRepository.saveAndFlush(new Wishlist(user, product))
            );
        } catch (DataIntegrityViolationException exception) {
            throw new WishlistException(
                    WishlistErrorCode.PRODUCT_ALREADY_WISHLISTED
            );
        }
    }

    @Override
    @Transactional
    public WishlistStatusResponse removeProduct(Long userId, Long productId) {
        Wishlist wishlist = wishlistRepository
                .findByUserIdAndProductId(userId, productId)
                .orElseThrow(() ->
                        new WishlistException(WishlistErrorCode.WISHLIST_ITEM_NOT_FOUND)
                );
        wishlistRepository.delete(wishlist);
        wishlistRepository.flush();
        return new WishlistStatusResponse(productId, false);
    }

    private WishlistItemResponse toResponse(Wishlist wishlist) {
        Product product = wishlist.getProduct();
        return new WishlistItemResponse(
                wishlist.getId(),
                product.getId(),
                product.getName(),
                product.getSlug(),
                product.getSku(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getThumbnailUrl(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getBrand().getId(),
                product.getBrand().getName(),
                true,
                wishlist.getCreatedAt()
        );
    }
}
