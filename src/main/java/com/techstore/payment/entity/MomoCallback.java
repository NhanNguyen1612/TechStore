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
@Table(name = "momo_callbacks")
@EntityListeners(AuditingEntityListener.class)
public class MomoCallback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @Enumerated(EnumType.STRING)
    @Column(name = "callback_type", nullable = false, length = 20)
    private MomoCallbackType callbackType;

    @Column(name = "partner_code", length = 50)
    private String partnerCode;

    @Column(name = "request_id", length = 50)
    private String requestId;

    @Column(name = "momo_order_id", length = 200)
    private String momoOrderId;

    private Long amount;

    @Column(name = "momo_transaction_id")
    private Long momoTransactionId;

    @Column(name = "result_code")
    private Integer resultCode;

    @Column(length = 1000)
    private String message;

    @Column(name = "signature_valid", nullable = false)
    private boolean signatureValid;

    @Column(nullable = false)
    private boolean processed;

    @Column(name = "processing_message", length = 1000)
    private String processingMessage;

    @Column(name = "raw_payload", columnDefinition = "TEXT", nullable = false)
    private String rawPayload;

    @CreatedDate
    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;

    protected MomoCallback() {
    }

    public MomoCallback(
            MomoCallbackType callbackType,
            String partnerCode,
            String requestId,
            String momoOrderId,
            Long amount,
            Long momoTransactionId,
            Integer resultCode,
            String message,
            boolean signatureValid,
            String rawPayload
    ) {
        this.callbackType = callbackType;
        this.partnerCode = partnerCode;
        this.requestId = requestId;
        this.momoOrderId = momoOrderId;
        this.amount = amount;
        this.momoTransactionId = momoTransactionId;
        this.resultCode = resultCode;
        this.message = message;
        this.signatureValid = signatureValid;
        this.rawPayload = rawPayload;
    }

    public void finish(Payment payment, boolean processed, String processingMessage) {
        this.payment = payment;
        this.processed = processed;
        this.processingMessage = processingMessage;
    }

    public boolean isSignatureValid() {
        return signatureValid;
    }

    public Long getId() {
        return id;
    }

    public MomoCallbackType getCallbackType() {
        return callbackType;
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

    public Integer getResultCode() {
        return resultCode;
    }

    public boolean isProcessed() {
        return processed;
    }

    public String getProcessingMessage() {
        return processingMessage;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }
}
