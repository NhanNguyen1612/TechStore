package com.techstore.admin.repository;

import com.techstore.auth.entity.Role;
import com.techstore.order.entity.OrderStatus;
import com.techstore.order.entity.PaymentMethod;
import com.techstore.payment.entity.PaymentStatus;
import com.techstore.product.entity.Product;
import com.techstore.review.entity.ReviewStatus;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class AdminDashboardRepository {

    private static final List<OrderStatus> COD_REVENUE_STATUSES =
            List.of(
                    OrderStatus.CONFIRMED,
                    OrderStatus.SHIPPING,
                    OrderStatus.DELIVERED,
                    OrderStatus.COMPLETED
            );

    private final EntityManager entityManager;

    public AdminDashboardRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public long users() {
        return scalar("""
                select count(user) from User user where user.deleted = false
                """, Long.class);
    }

    public long users(Role role, Instant from, Instant to) {
        return entityManager.createQuery("""
                        select count(user) from User user
                        where user.deleted = false
                          and user.role = :role
                          and (:from is null or user.createdAt >= :from)
                          and (:to is null or user.createdAt < :to)
                        """, Long.class)
                .setParameter("role", role)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();
    }

    public long products(Boolean active) {
        String jpql = active == null
                ? "select count(product) from Product product where product.deleted = false"
                : """
                  select count(product) from Product product
                  where product.deleted = false and product.active = :active
                  """;
        var query = entityManager.createQuery(jpql, Long.class);
        if (active != null) {
            query.setParameter("active", active);
        }
        return query.getSingleResult();
    }

    public long orders(OrderStatus status, Instant from, Instant to) {
        return entityManager.createQuery("""
                        select count(orders) from Order orders
                        where (:status is null or orders.status = :status)
                          and (:from is null or orders.createdAt >= :from)
                          and (:to is null or orders.createdAt < :to)
                        """, Long.class)
                .setParameter("status", status)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();
    }

    public long ordersInStatuses(List<OrderStatus> statuses) {
        return entityManager.createQuery("""
                        select count(orders) from Order orders
                        where orders.status in :statuses
                        """, Long.class)
                .setParameter("statuses", statuses)
                .getSingleResult();
    }

    public List<Object[]> orderEvents(Instant from, Instant to) {
        return entityManager.createQuery("""
                        select orders.createdAt, orders.status
                        from Order orders
                        where (:from is null or orders.createdAt >= :from)
                          and (:to is null or orders.createdAt < :to)
                        order by orders.createdAt
                        """, Object[].class)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
    }

    public List<Object[]> momoRevenueEvents(Instant from, Instant to) {
        return entityManager.createQuery("""
                        select payment.paidAt, payment.amount
                        from Payment payment
                        where payment.status = :status
                          and payment.order.status <> :cancelled
                          and (:from is null or payment.paidAt >= :from)
                          and (:to is null or payment.paidAt < :to)
                        """, Object[].class)
                .setParameter("status", PaymentStatus.PAID)
                .setParameter("cancelled", OrderStatus.CANCELLED)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
    }

    public List<Object[]> codRevenueEvents(Instant from, Instant to) {
        return entityManager.createQuery("""
                        select coalesce(
                                   orders.confirmedAt,
                                   orders.deliveredAt,
                                   orders.createdAt
                               ),
                               orders.totalAmount
                        from Order orders
                        where orders.status in :statuses
                          and (
                              orders.paymentMethod = :codMethod
                              or orders.paymentMethod is null
                          )
                          and not exists (
                            select payment.id from Payment payment
                            where payment.order.id = orders.id
                          )
                          and (
                              :from is null
                              or coalesce(
                                  orders.confirmedAt,
                                  orders.deliveredAt,
                                  orders.createdAt
                              ) >= :from
                          )
                          and (
                              :to is null
                              or coalesce(
                                  orders.confirmedAt,
                                  orders.deliveredAt,
                                  orders.createdAt
                              ) < :to
                          )
                        """, Object[].class)
                .setParameter("statuses", COD_REVENUE_STATUSES)
                .setParameter("codMethod", PaymentMethod.COD)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
    }

    public List<Product> productsBySold(int limit) {
        return entityManager.createQuery("""
                        select product from Product product
                        where product.deleted = false
                        order by product.soldCount desc, product.id asc
                        """, Product.class)
                .setMaxResults(limit)
                .getResultList();
    }

    public List<Product> lowStock(int threshold, boolean outOfStock, int limit) {
        String operator = outOfStock
                ? "product.stockQuantity = 0"
                : "product.stockQuantity > 0 and product.stockQuantity <= :threshold";
        var query = entityManager.createQuery("""
                        select product from Product product
                        where product.deleted = false and %s
                        order by product.stockQuantity asc, product.id asc
                        """.formatted(operator), Product.class);
        if (!outOfStock) {
            query.setParameter("threshold", threshold);
        }
        return query.setMaxResults(limit).getResultList();
    }

    public long paymentCount(PaymentStatus status) {
        return entityManager.createQuery("""
                        select count(payment) from Payment payment
                        where (:status is null or payment.status = :status)
                        """, Long.class)
                .setParameter("status", status)
                .getSingleResult();
    }

    public long codOrders() {
        return scalar("""
                select count(orders) from Order orders
                where not exists (
                    select payment.id from Payment payment where payment.order.id = orders.id
                )
                """, Long.class);
    }

    public long reviewCount(ReviewStatus status) {
        return entityManager.createQuery("""
                        select count(review) from Review review
                        where (:status is null or review.status = :status)
                        """, Long.class)
                .setParameter("status", status)
                .getSingleResult();
    }

    public BigDecimal averageRating() {
        Double value = scalar("select avg(review.rating) from Review review", Double.class);
        return value == null ? BigDecimal.ZERO : BigDecimal.valueOf(value);
    }

    public List<Object[]> reviewsByRating() {
        return entityManager.createQuery("""
                        select review.rating, count(review)
                        from Review review
                        group by review.rating
                        order by review.rating
                        """, Object[].class)
                .getResultList();
    }

    public long totalStock() {
        Long value = scalar("""
                select sum(product.stockQuantity) from Product product
                where product.deleted = false
                """, Long.class);
        return value == null ? 0 : value;
    }

    public long lowStockCount(int threshold) {
        return entityManager.createQuery("""
                        select count(product) from Product product
                        where product.deleted = false
                          and product.stockQuantity > 0
                          and product.stockQuantity <= :threshold
                        """, Long.class)
                .setParameter("threshold", threshold)
                .getSingleResult();
    }

    public long outOfStockCount() {
        return scalar("""
                select count(product) from Product product
                where product.deleted = false and product.stockQuantity = 0
                """, Long.class);
    }

    public BigDecimal inventoryValue() {
        BigDecimal value = scalar("""
                select sum(product.price * product.stockQuantity)
                from Product product where product.deleted = false
                """, BigDecimal.class);
        return value == null ? BigDecimal.ZERO : value;
    }

    private <T> T scalar(String jpql, Class<T> type) {
        return entityManager.createQuery(jpql, type).getSingleResult();
    }
}
