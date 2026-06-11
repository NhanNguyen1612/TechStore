package com.techstore.product.repository;

import com.techstore.product.entity.Product;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.query.Param;

public interface ProductRepository
        extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    @Override
    @EntityGraph(attributePaths = {"category", "brand"})
    Page<Product> findAll(Specification<Product> specification, Pageable pageable);

    @EntityGraph(attributePaths = {"category", "brand", "images"})
    Optional<Product> findByIdAndDeletedFalse(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select product from Product product where product.id in :ids")
    List<Product> findAllByIdInForUpdate(@Param("ids") Collection<Long> ids);

    boolean existsBySkuIgnoreCaseAndDeletedFalse(String sku);

    boolean existsBySkuIgnoreCaseAndDeletedFalseAndIdNot(String sku, Long id);

    boolean existsBySlugIgnoreCaseAndDeletedFalse(String slug);

    boolean existsBySlugIgnoreCaseAndDeletedFalseAndIdNot(String slug, Long id);

    long countByCategoryIdAndDeletedFalse(Long categoryId);

    long countByBrandIdAndDeletedFalse(Long brandId);
}
