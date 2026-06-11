package com.techstore.brand.repository;

import com.techstore.brand.entity.Brand;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BrandRepository extends JpaRepository<Brand, Long> {

    @Query("""
            select brand
            from Brand brand
            where brand.deleted = false
              and (
                    :search = ''
                    or lower(brand.name) like lower(concat('%', :search, '%'))
                    or lower(coalesce(brand.description, ''))
                        like lower(concat('%', :search, '%'))
                  )
            """)
    Page<Brand> searchActive(
            @Param("search") String search,
            Pageable pageable
    );

    Optional<Brand> findByIdAndDeletedFalse(Long id);

    boolean existsByNameIgnoreCaseAndDeletedFalse(String name);

    boolean existsByNameIgnoreCaseAndDeletedFalseAndIdNot(String name, Long id);

    boolean existsBySlugIgnoreCaseAndDeletedFalse(String slug);

    boolean existsBySlugIgnoreCaseAndDeletedFalseAndIdNot(String slug, Long id);
}
