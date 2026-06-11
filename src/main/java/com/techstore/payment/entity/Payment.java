package com.techstore.payment.entity;

import com.techstore.order.entity.Order;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(
        name = "payments",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_payments_order", columnNames = "order_id"),
            @UniqueConstraint(
                    name = "uk_payments_momo_order_id",
                    columnNames = "momo_order_id"
            ),
            @UniqueConstraint(
                    name = "uk_payments_request_id",
                    columnNames = "request_id"
            )
        }
)
@EntityListeners(AuditingEntityListener.class)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false, length = 20)
    private String provider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(nullable = false)
    private long amount;

    @Column(name = "request_id", length = 50)
    private String requestId;

    @Column(name = "momo_order_id", length = 200)
    private String momoOrderId;

    @Column(name = "momo_transaction_id")
    private Long momoTransactionId;

    @Column(name = "pay_url", length = 2048)
    private String payUrl;

    @Column(name = "deeplink", length = 2048)
    private String deeplink;

    @Column(name = "qr_code_url", columnDefinition = "TEXT")
    private String qrCodeUrl;

    @Column(name = "result_code")
    private Integer resultCode;

    @Column(length = 1000)
    private String message;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected Payment() {
    }

    public Payment(Order order, long amount) {
        this.order = order;
        this.provider = "MOMO";
        this.status = PaymentStatus.PENDING;
        this.amount = amount;
    }

    public void beginAttempt(String requestId, String momoOrderId) {
        beginAttempt(requestId, momoOrderId, null);
    }

    public void beginAttempt(
            String requestId,
            String momoOrderId,
            Instant expiresAt
    ) {
        this.requestId = requestId;
        this.momoOrderId = momoOrderId;
        this.momoTransactionId = null;
        this.payUrl = null;
        this.deeplink = null;
        this.qrCodeUrl = null;
        this.resultCode = null;
        this.message = null;
        this.paidAt = null;
        this.expiresAt = expiresAt;
        this.status = PaymentStatus.PENDING;
    }

    public void acceptCreateResponse(
            String payUrl,
            String deeplink,
            String qrCodeUrl,
            Integer resultCode,
            String message
    ) {
        this.payUrl = payUrl;
        this.deeplink = deeplink;
        this.qrCodeUrl = qrCodeUrl;
        this.resultCode = resultCode;
        this.message = message;
    }

    public void markPaid(Long transactionId, Integer resultCode, String message, Instant paidAt) {
        this.momoTransactionId = transactionId;
        this.resultCode = resultCode;
        this.message = message;
        this.status = PaymentStatus.PAID;
        this.paidAt = paidAt;
    }

    public boolean hasActiveCheckout(Instant now) {
        return status == PaymentStatus.PENDING
                && payUrl != null
                && !payUrl.isBlank()
                && expiresAt != null
                && expiresAt.isAfter(now);
    }

    public boolean expireIfNecessary(Instant now) {
        if (status != PaymentStatus.PENDING
                || expiresAt == null
                || expiresAt.isAfter(now)) {
            return false;
        }
        status = PaymentStatus.FAILED;
        resultCode = null;
        message = "MoMo QR expired";
        payUrl = null;
        deeplink = null;
        qrCodeUrl = null;
        return true;
    }

    public void markPending(Long transactionId, Integer resultCode, String message) {
        if (status != PaymentStatus.PAID && status != PaymentStatus.REFUNDED) {
            this.momoTransactionId = transactionId;
            this.resultCode = resultCode;
            this.message = message;
            this.status = PaymentStatus.PENDING;
        }
    }

    public void markFailed(Integer resultCode, String message) {
        if (status != PaymentStatus.PAID && status != PaymentStatus.REFUNDED) {
            this.resultCode = resultCode;
            this.message = message;
            this.status = PaymentStatus.FAILED;
        }
    }

    public void markCancelled(Integer resultCode, String message) {
        if (status != PaymentStatus.PAID && status != PaymentStatus.REFUNDED) {
            this.resultCode = resultCode;
            this.message = message;
            this.status = PaymentStatus.CANCELLED;
        }
    }

    public void cancelWithOrder() {
        if (status == PaymentStatus.PENDING || status == PaymentStatus.FAILED) {
            status = PaymentStatus.CANCELLED;
        }
    }

    public void changeStatus(PaymentStatus status, Instant changedAt) {
        this.status = status;
        if (status == PaymentStatus.PAID && paidAt == null) {
            paidAt = changedAt;
        } else if (status != PaymentStatus.PAID) {
            paidAt = null;
        }
    }

    public Long getId() {
        return id;
    }

    public Order getOrder() {
        return order;
    }

    public String getProvider() {
        return provider;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public long getAmount() {
        return amount;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getMomoOrderId() {
        return momoOrderId;
    }

    public Long getMomoTransactionId() {
        return momoTransactionId;
    }

    public String getPayUrl() {
        return payUrl;
    }

    public String getDeeplink() {
        return deeplink;
    }

    public String getQrCodeUrl() {
        return qrCodeUrl;
    }

    public Integer getResultCode() {
        return resultCode;
    }

    public String getMessage() {
        return message;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
