package com.techstore.dashboard.repository;

import com.techstore.auth.entity.Role;
import com.techstore.dashboard.dto.response.OrderStatusStatisticResponse;
import com.techstore.dashboard.dto.response.TopCustomerResponse;
import com.techstore.dashboard.dto.response.TopProductResponse;
import com.techstore.order.entity.OrderStatus;
import com.techstore.order.entity.PaymentMethod;
import com.techstore.payment.entity.PaymentStatus;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class DashboardQueryRepository {

    private static final List<OrderStatus> COD_REVENUE_STATUSES = List.of(
            OrderStatus.CONFIRMED,
            OrderStatus.SHIPPING,
            OrderStatus.DELIVERED,
            OrderStatus.COMPLETED
    );

    private final EntityManager entityManager;

    public DashboardQueryRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public BigDecimal momoRevenue(Instant from, Instant to) {
        Long amount = entityManager.createQuery("""
                        select sum(payment.amount)
                        from Payment payment
                        where payment.status = :paidStatus
                          and payment.order.status <> :cancelledStatus
                          and (:from is null or payment.paidAt >= :from)
                          and (:to is null or payment.paidAt < :to)
                        """, Long.class)
                .setParameter("paidStatus", PaymentStatus.PAID)
                .setParameter("cancelledStatus", OrderStatus.CANCELLED)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();
        return BigDecimal.valueOf(amount == null ? 0L : amount).setScale(2);
    }

    public BigDecimal codRevenue(Instant from, Instant to) {
        BigDecimal amount = entityManager.createQuery("""
                        select sum(orders.totalAmount)
                        from Order orders
                        where orders.status in :statuses
                          and (
                              orders.paymentMethod = :codMethod
                              or orders.paymentMethod is null
                          )
                          and not exists (
                              select payment.id
                              from Payment payment
                              where payment.order = orders
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
                        """, BigDecimal.class)
                .setParameter("statuses", COD_REVENUE_STATUSES)
                .setParameter("codMethod", PaymentMethod.COD)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();
        return money(amount);
    }

    public long totalOrders(Instant from, Instant to) {
        return entityManager.createQuery("""
                        select count(orders)
                        from Order orders
                        where (:from is null or orders.createdAt >= :from)
                          and (:to is null or orders.createdAt < :to)
                        """, Long.class)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();
    }

    public long totalUsers() {
        return entityManager.createQuery(
                        "select count(user) from User user",
                        Long.class
                )
                .getSingleResult();
    }

    public long totalCustomers() {
        return entityManager.createQuery("""
                        select count(user)
                        from User user
                        where user.role = :role
                        """, Long.class)
                .setParameter("role", Role.ROLE_CUSTOMER)
                .getSingleResult();
    }

    public long newCustomers(Instant from, Instant to) {
        return entityManager.createQuery("""
                        select count(user)
                        from User user
                        where user.role = :role
                          and (:from is null or user.createdAt >= :from)
                          and (:to is null or user.createdAt < :to)
                        """, Long.class)
                .setParameter("role", Role.ROLE_CUSTOMER)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();
    }

    public List<OrderStatusStatisticResponse> orderStatistics(
            Instant from,
            Instant to
    ) {
        List<Object[]> rows = entityManager.createQuery("""
                        select orders.status, count(orders)
                        from Order orders
                        where (:from is null or orders.createdAt >= :from)
                          and (:to is null or orders.createdAt < :to)
                        group by orders.status
                        """, Object[].class)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
        return Arrays.stream(OrderStatus.values())
                .map(status -> new OrderStatusStatisticResponse(
                        status,
                        countForStatus(rows, status)
                ))
                .toList();
    }

    public long totalProducts() {
        return entityManager.createQuery("""
                        select count(product)
                        from Product product
                        where product.deleted = false
                        """, Long.class)
                .getSingleResult();
    }

    public long outOfStockProducts() {
        return entityManager.createQuery("""
                        select count(product)
                        from Product product
                        where product.deleted = false
                          and product.stockQuantity = 0
                        """, Long.class)
                .getSingleResult();
    }

    public List<TopProductResponse> topProducts(
            Instant from,
            Instant to,
            int limit
    ) {
        List<Object[]> rows = entityManager.createQuery("""
                        select item.product.id,
                               item.productName,
                               item.productSku,
                               item.thumbnailUrl,
                               sum(item.quantity),
                               sum(item.subtotal)
                        from OrderItem item
                        where (
                            exists (
                                select payment.id
                                from Payment payment
                                where payment.order = item.order
                                  and payment.status = :paidStatus
                                  and (:from is null or payment.paidAt >= :from)
                                  and (:to is null or payment.paidAt < :to)
                            )
                            or (
                                item.order.status in :codStatuses
                                and (
                                    item.order.paymentMethod = :codMethod
                                    or item.order.paymentMethod is null
                                )
                                and not exists (
                                    select payment.id
                                    from Payment payment
                                    where payment.order = item.order
                                )
                                and (
                                    :from is null
                                    or coalesce(
                                        item.order.confirmedAt,
                                        item.order.deliveredAt,
                                        item.order.createdAt
                                    ) >= :from
                                )
                                and (
                                    :to is null
                                    or coalesce(
                                        item.order.confirmedAt,
                                        item.order.deliveredAt,
                                        item.order.createdAt
                                    ) < :to
                                )
                            )
                        )
                          and item.order.status <> :cancelledStatus
                        group by item.product.id,
                                 item.productName,
                                 item.productSku,
                                 item.thumbnailUrl
                        order by sum(item.quantity) desc, sum(item.subtotal) desc
                        """, Object[].class)
                .setParameter("paidStatus", PaymentStatus.PAID)
                .setParameter("codStatuses", COD_REVENUE_STATUSES)
                .setParameter("codMethod", PaymentMethod.COD)
                .setParameter("cancelledStatus", OrderStatus.CANCELLED)
                .setParameter("from", from)
                .setParameter("to", to)
                .setMaxResults(limit)
                .getResultList();
        return rows.stream()
                .map(row -> new TopProductResponse(
                        (Long) row[0],
                        (String) row[1],
                        (String) row[2],
                        (String) row[3],
                        ((Number) row[4]).longValue(),
                        money((BigDecimal) row[5])
                ))
                .toList();
    }

    public List<TopCustomerResponse> topCustomers(
            Instant from,
            Instant to,
            int limit
    ) {
        List<Object[]> rows = entityManager.createQuery("""
                        select orders.user.id,
                               orders.user.fullName,
                               orders.user.email,
                               count(orders),
                               sum(orders.totalAmount)
                        from Order orders
                        where orders.user.role = :customerRole
                          and (
                              exists (
                                  select payment.id
                                  from Payment payment
                                  where payment.order = orders
                                    and payment.status = :paidStatus
                                    and (:from is null or payment.paidAt >= :from)
                                    and (:to is null or payment.paidAt < :to)
                              )
                              or (
                                  orders.status in :codStatuses
                                  and (
                                      orders.paymentMethod = :codMethod
                                      or orders.paymentMethod is null
                                  )
                                  and not exists (
                                      select payment.id
                                      from Payment payment
                                      where payment.order = orders
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
                              )
                          )
                          and orders.status <> :cancelledStatus
                        group by orders.user.id,
                                 orders.user.fullName,
                                 orders.user.email
                        order by sum(orders.totalAmount) desc, count(orders) desc
                        """, Object[].class)
                .setParameter("customerRole", Role.ROLE_CUSTOMER)
                .setParameter("paidStatus", PaymentStatus.PAID)
                .setParameter("codStatuses", COD_REVENUE_STATUSES)
                .setParameter("codMethod", PaymentMethod.COD)
                .setParameter("cancelledStatus", OrderStatus.CANCELLED)
                .setParameter("from", from)
                .setParameter("to", to)
                .setMaxResults(limit)
                .getResultList();
        return rows.stream()
                .map(row -> new TopCustomerResponse(
                        (Long) row[0],
                        (String) row[1],
                        (String) row[2],
                        ((Number) row[3]).longValue(),
                        money((BigDecimal) row[4])
                ))
                .toList();
    }

    private long countForStatus(List<Object[]> rows, OrderStatus status) {
        return rows.stream()
                .filter(row -> row[0] == status)
                .map(row -> ((Number) row[1]).longValue())
                .findFirst()
                .orElse(0L);
    }

    private BigDecimal money(BigDecimal amount) {
        return (amount == null ? BigDecimal.ZERO : amount).setScale(2);
    }
}
