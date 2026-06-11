package com.techstore.category.repository;

import com.techstore.category.entity.Category;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Query("""
            select category
            from Category category
            where category.deleted = false
              and (
                    :search = ''
                    or lower(category.name) like lower(concat('%', :search, '%'))
                    or lower(coalesce(category.description, ''))
                        like lower(concat('%', :search, '%'))
                  )
            """)
    Page<Category> searchActive(
            @Param("search") String search,
            Pageable pageable
    );

    Optional<Category> findByIdAndDeletedFalse(Long id);

    boolean existsByNameIgnoreCaseAndDeletedFalse(String name);

    boolean existsByNameIgnoreCaseAndDeletedFalseAndIdNot(String name, Long id);

    boolean existsBySlugIgnoreCaseAndDeletedFalse(String slug);

    boolean existsBySlugIgnoreCaseAndDeletedFalseAndIdNot(String slug, Long id);
}
