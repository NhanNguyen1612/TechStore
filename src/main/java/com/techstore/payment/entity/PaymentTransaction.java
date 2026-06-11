package com.techstore.payment.entity;

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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "payment_transactions")
@EntityListeners(AuditingEntityListener.class)
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private PaymentTransactionType transactionType;

    @Column(name = "request_id", length = 50)
    private String requestId;

    @Column(name = "momo_order_id", length = 200)
    private String momoOrderId;

    @Column(name = "momo_transaction_id")
    private Long momoTransactionId;

    @Column(nullable = false)
    private long amount;

    @Column(name = "result_code")
    private Integer resultCode;

    @Column(length = 1000)
    private String message;

    @Column(name = "request_payload", columnDefinition = "TEXT")
    private String requestPayload;

    @Column(name = "response_payload", columnDefinition = "TEXT")
    private String responsePayload;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PaymentTransaction() {
    }

    public PaymentTransaction(
            Payment payment,
            PaymentTransactionType transactionType,
            String requestId,
            String momoOrderId,
            Long momoTransactionId,
            long amount,
            Integer resultCode,
            String message,
            String requestPayload,
            String responsePayload
    ) {
        this.payment = payment;
        this.transactionType = transactionType;
        this.requestId = requestId;
        this.momoOrderId = momoOrderId;
        this.momoTransactionId = momoTransactionId;
        this.amount = amount;
        this.resultCode = resultCode;
        this.message = message;
        this.requestPayload = requestPayload;
        this.responsePayload = responsePayload;
    }

    public Long getId() {
        return id;
    }

    public Payment getPayment() {
        return payment;
    }

    public PaymentTransactionType getTransactionType() {
        return transactionType;
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

    public long getAmount() {
        return amount;
    }

    public Integer getResultCode() {
        return resultCode;
    }

    public String getMessage() {
        return message;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
