package com.techstore.wishlist.repository;

import com.techstore.wishlist.entity.Wishlist;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    @EntityGraph(attributePaths = {"product", "product.category", "product.brand"})
    List<Wishlist> findAllByUserIdAndProductDeletedFalseOrderByCreatedAtDesc(Long userId);

    @EntityGraph(attributePaths = {"product"})
    Optional<Wishlist> findByUserIdAndProductId(Long userId, Long productId);

    boolean existsByUserIdAndProductId(Long userId, Long productId);
}
