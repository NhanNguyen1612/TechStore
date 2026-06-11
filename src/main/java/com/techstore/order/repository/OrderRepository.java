package com.techstore.order.repository;

import com.techstore.order.entity.Order;
import com.techstore.order.entity.OrderStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {

    boolean existsByOrderCode(String orderCode);

    @EntityGraph(attributePaths = {"user", "items", "items.product"})
    Optional<Order> findByOrderCodeIgnoreCase(String orderCode);

    @EntityGraph(attributePaths = {"user"})
    List<Order> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"user"})
    List<Order> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    @EntityGraph(attributePaths = {"user", "items", "items.product"})
    @Query("select orders from Order orders where orders.id = :id")
    Optional<Order> findDetailById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"user", "items", "items.product"})
    @Query("select orders from Order orders where orders.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            select case when count(orders) > 0 then true else false end
            from Order orders
            join orders.items item
            where orders.user.id = :userId
              and item.product.id = :productId
              and orders.status in :statuses
            """)
    boolean hasPurchasedProduct(
            @Param("userId") Long userId,
            @Param("productId") Long productId,
            @Param("statuses") Set<OrderStatus> statuses
    );
}
