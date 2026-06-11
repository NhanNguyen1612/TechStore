package com.techstore.product.repository;

import com.techstore.product.entity.Product;
import jakarta.persistence.criteria.JoinType;
import java.math.BigDecimal;
import org.springframework.data.jpa.domain.Specification;

public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    public static Specification<Product> availableProducts() {
        return (root, query, builder) -> builder.and(
                builder.isFalse(root.get("deleted")),
                builder.isTrue(root.get("active")),
                builder.isFalse(root.join("category", JoinType.INNER).get("deleted")),
                builder.isFalse(root.join("brand", JoinType.INNER).get("deleted"))
        );
    }

    public static Specification<Product> matches(String keyword) {
        return (root, query, builder) -> {
            if (keyword == null || keyword.isBlank()) {
                return builder.conjunction();
            }
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            return builder.or(
                    builder.like(builder.lower(root.get("name")), pattern),
                    builder.like(builder.lower(root.get("sku")), pattern),
                    builder.like(
                            builder.lower(builder.coalesce(root.get("description"), "")),
                            pattern
                    )
            );
        };
    }

    public static Specification<Product> hasCategory(Long categoryId) {
        return (root, query, builder) -> categoryId == null
                ? builder.conjunction()
                : builder.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Product> hasBrand(Long brandId) {
        return (root, query, builder) -> brandId == null
                ? builder.conjunction()
                : builder.equal(root.get("brand").get("id"), brandId);
    }

    public static Specification<Product> priceAtLeast(BigDecimal minPrice) {
        return (root, query, builder) -> minPrice == null
                ? builder.conjunction()
                : builder.greaterThanOrEqualTo(root.get("price"), minPrice);
    }

    public static Specification<Product> priceAtMost(BigDecimal maxPrice) {
        return (root, query, builder) -> maxPrice == null
                ? builder.conjunction()
                : builder.lessThanOrEqualTo(root.get("price"), maxPrice);
    }
}
