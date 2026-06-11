package com.techstore.admin.repository;

import com.techstore.admin.entity.Coupon;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    @Query("""
            select coupon from Coupon coupon
            where coupon.deleted = false
              and (:search = '' or lower(coupon.code) like lower(concat('%', :search, '%'))
                   or lower(coupon.name) like lower(concat('%', :search, '%')))
              and (:active is null or coupon.active = :active)
            """)
    Page<Coupon> search(
            @Param("search") String search,
            @Param("active") Boolean active,
            Pageable pageable
    );

    Optional<Coupon> findByIdAndDeletedFalse(Long id);

    boolean existsByCodeIgnoreCaseAndDeletedFalse(String code);

    boolean existsByCodeIgnoreCaseAndDeletedFalseAndIdNot(String code, Long id);
}
