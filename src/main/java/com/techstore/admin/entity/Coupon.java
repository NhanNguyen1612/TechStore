package com.techstore.admin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(
        name = "coupons",
        uniqueConstraints = @UniqueConstraint(name = "uk_coupons_code", columnNames = "code")
)
@EntityListeners(AuditingEntityListener.class)
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CouponType type;

    @Column(name = "discount_value", nullable = false, precision = 19, scale = 2)
    private BigDecimal value;

    @Column(name = "minimum_order_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal minimumOrderAmount;

    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Column(name = "used_count", nullable = false)
    private int usedCount;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private boolean deleted;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected Coupon() {
    }

    public Coupon(
            String code,
            String name,
            CouponType type,
            BigDecimal value,
            BigDecimal minimumOrderAmount,
            Integer usageLimit,
            Instant startsAt,
            Instant endsAt
    ) {
        update(code, name, type, value, minimumOrderAmount, usageLimit, startsAt, endsAt);
    }

    public void update(
            String code,
            String name,
            CouponType type,
            BigDecimal value,
            BigDecimal minimumOrderAmount,
            Integer usageLimit,
            Instant startsAt,
            Instant endsAt
    ) {
        this.code = code;
        this.name = name;
        this.type = type;
        this.value = value;
        this.minimumOrderAmount = minimumOrderAmount;
        this.usageLimit = usageLimit;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
    }

    public void changeStatus(boolean active) {
        this.active = active;
    }

    public void softDelete(Instant deletedAt) {
        this.deleted = true;
        this.active = false;
        this.deletedAt = deletedAt;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public CouponType getType() {
        return type;
    }

    public BigDecimal getValue() {
        return value;
    }

    public BigDecimal getMinimumOrderAmount() {
        return minimumOrderAmount;
    }

    public Integer getUsageLimit() {
        return usageLimit;
    }

    public int getUsedCount() {
        return usedCount;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public Instant getEndsAt() {
        return endsAt;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
