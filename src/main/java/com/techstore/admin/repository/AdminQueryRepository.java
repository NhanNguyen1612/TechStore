package com.techstore.admin.repository;

import com.techstore.auth.entity.Role;
import com.techstore.auth.entity.User;
import com.techstore.chat.entity.Conversation;
import com.techstore.order.entity.Order;
import com.techstore.order.entity.OrderStatus;
import com.techstore.order.entity.PaymentMethod;
import com.techstore.payment.entity.Payment;
import com.techstore.payment.entity.PaymentStatus;
import com.techstore.product.entity.Product;
import com.techstore.review.entity.Review;
import com.techstore.review.entity.ReviewStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public class AdminQueryRepository {

    private final EntityManager entityManager;

    public AdminQueryRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Page<User> users(
            String search,
            Role role,
            Boolean active,
            Pageable pageable
    ) {
        StringBuilder where = new StringBuilder(" where user.deleted = false");
        Map<String, Object> parameters = new LinkedHashMap<>();
        if (hasText(search)) {
            where.append("""
                     and (
                       lower(user.fullName) like :search
                       or lower(user.email) like :search
                       or lower(coalesce(user.phone, '')) like :search
                     )
                    """);
            parameters.put("search", like(search));
        }
        if (role != null) {
            where.append(" and user.role = :role");
            parameters.put("role", role);
        }
        if (active != null) {
            where.append(" and user.enabled = :active");
            parameters.put("active", active);
        }
        return page(
                "select user from User user" + where,
                "select count(user) from User user" + where,
                User.class,
                parameters,
                pageable,
                "user"
        );
    }

    public Page<Product> products(
            String search,
            Long categoryId,
            Long brandId,
            Boolean active,
            Pageable pageable
    ) {
        StringBuilder where = new StringBuilder(" where product.deleted = false");
        Map<String, Object> parameters = new LinkedHashMap<>();
        if (hasText(search)) {
            where.append("""
                     and (
                       lower(product.name) like :search
                       or lower(product.sku) like :search
                     )
                    """);
            parameters.put("search", like(search));
        }
        if (categoryId != null) {
            where.append(" and product.category.id = :categoryId");
            parameters.put("categoryId", categoryId);
        }
        if (brandId != null) {
            where.append(" and product.brand.id = :brandId");
            parameters.put("brandId", brandId);
        }
        if (active != null) {
            where.append(" and product.active = :active");
            parameters.put("active", active);
        }
        return page(
                "select product from Product product" + where,
                "select count(product) from Product product" + where,
                Product.class,
                parameters,
                pageable,
                "product"
        );
    }

    public Page<Order> orders(
            OrderStatus status,
            String paymentMethod,
            Instant from,
            Instant to,
            Pageable pageable
    ) {
        StringBuilder where = new StringBuilder(" where 1 = 1");
        Map<String, Object> parameters = new LinkedHashMap<>();
        if (status != null) {
            where.append(" and orders.status = :status");
            parameters.put("status", status);
        }
        if ("MOMO".equalsIgnoreCase(paymentMethod)) {
            where.append(" and orders.paymentMethod = :paymentMethod");
            parameters.put("paymentMethod", PaymentMethod.MOMO);
        } else if ("COD".equalsIgnoreCase(paymentMethod)) {
            where.append("""
                     and (
                       orders.paymentMethod = :paymentMethod
                       or orders.paymentMethod is null
                     )
                    """);
            parameters.put("paymentMethod", PaymentMethod.COD);
        }
        appendDateRange(where, parameters, "orders.createdAt", from, to);
        return page(
                "select orders from Order orders" + where,
                "select count(orders) from Order orders" + where,
                Order.class,
                parameters,
                pageable,
                "orders"
        );
    }

    public Page<Payment> payments(
            PaymentStatus status,
            Instant from,
            Instant to,
            Pageable pageable
    ) {
        StringBuilder where = new StringBuilder(" where 1 = 1");
        Map<String, Object> parameters = new LinkedHashMap<>();
        if (status != null) {
            where.append(" and payment.status = :status");
            parameters.put("status", status);
        }
        appendDateRange(where, parameters, "payment.createdAt", from, to);
        return page(
                "select payment from Payment payment" + where,
                "select count(payment) from Payment payment" + where,
                Payment.class,
                parameters,
                pageable,
                "payment"
        );
    }

    public Page<Review> reviews(
            Integer rating,
            ReviewStatus status,
            Pageable pageable
    ) {
        StringBuilder where = new StringBuilder(" where 1 = 1");
        Map<String, Object> parameters = new LinkedHashMap<>();
        if (rating != null) {
            where.append(" and review.rating = :rating");
            parameters.put("rating", rating);
        }
        if (status != null) {
            where.append(" and review.status = :status");
            parameters.put("status", status);
        }
        return page(
                "select review from Review review" + where,
                "select count(review) from Review review" + where,
                Review.class,
                parameters,
                pageable,
                "review"
        );
    }

    public Page<Conversation> conversations(Boolean closed, Pageable pageable) {
        String where = closed == null ? "" : " where conversation.closed = :closed";
        Map<String, Object> parameters = closed == null
                ? Map.of()
                : Map.of("closed", closed);
        return page(
                "select conversation from Conversation conversation" + where,
                "select count(conversation) from Conversation conversation" + where,
                Conversation.class,
                parameters,
                pageable,
                "conversation"
        );
    }

    private <T> Page<T> page(
            String selectJpql,
            String countJpql,
            Class<T> type,
            Map<String, Object> parameters,
            Pageable pageable,
            String alias
    ) {
        TypedQuery<T> query = entityManager.createQuery(
                selectJpql + orderBy(pageable, alias),
                type
        );
        TypedQuery<Long> count = entityManager.createQuery(countJpql, Long.class);
        parameters.forEach((name, value) -> {
            query.setParameter(name, value);
            count.setParameter(name, value);
        });
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());
        return new PageImpl<>(
                query.getResultList(),
                pageable,
                count.getSingleResult()
        );
    }

    private String orderBy(Pageable pageable, String alias) {
        if (pageable.getSort().isUnsorted()) {
            return " order by " + alias + ".createdAt desc";
        }
        StringBuilder order = new StringBuilder(" order by ");
        pageable.getSort().forEach(item -> {
            if (order.length() > 10) {
                order.append(", ");
            }
            order.append(alias)
                    .append('.')
                    .append(item.getProperty())
                    .append(' ')
                    .append(item.getDirection().name());
        });
        return order.toString();
    }

    private void appendDateRange(
            StringBuilder where,
            Map<String, Object> parameters,
            String field,
            Instant from,
            Instant to
    ) {
        if (from != null) {
            where.append(" and ").append(field).append(" >= :from");
            parameters.put("from", from);
        }
        if (to != null) {
            where.append(" and ").append(field).append(" < :to");
            parameters.put("to", to);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String like(String value) {
        return "%" + value.trim().toLowerCase() + "%";
    }
}
