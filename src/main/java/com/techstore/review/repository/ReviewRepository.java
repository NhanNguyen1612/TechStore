package com.techstore.review.repository;

import com.techstore.review.entity.Review;
import com.techstore.review.entity.ReviewStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByUserIdAndProductId(Long userId, Long productId);

    @EntityGraph(attributePaths = {"user", "product", "images"})
    @Query("select review from Review review where review.id = :id")
    Optional<Review> findDetailById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"user", "product", "images"})
    List<Review> findAllByProductIdAndStatusOrderByCreatedAtDesc(
            Long productId,
            ReviewStatus status
    );
}
