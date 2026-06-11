package com.techstore.order.entity;

import com.techstore.auth.entity.User;
import com.techstore.product.entity.Product;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(
        name = "orders",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_orders_order_code",
                columnNames = "order_code"
        )
)
@EntityListeners(AuditingEntityListener.class)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_code", nullable = false, length = 40)
    private String orderCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status;

    @Column(name = "total_quantity", nullable = false)
    private int totalQuantity;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "recipient_name", length = 100)
    private String recipientName;

    @Column(length = 20)
    private String phone;

    @Column(name = "shipping_address", length = 500)
    private String shippingAddress;

    @Column(length = 1000)
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 20)
    private PaymentMethod paymentMethod;

    @OneToMany(
            mappedBy = "order",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("id ASC")
    private final List<OrderItem> items = new ArrayList<>();

    @OneToMany(
            mappedBy = "order",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("createdAt ASC, id ASC")
    private final List<OrderStatusHistory> statusHistory = new ArrayList<>();

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "shipped_at")
    private Instant shippedAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected Order() {
    }

    public Order(String orderCode, User user) {
        this(orderCode, user, user.getFullName(), user.getPhone(), null, null,
                PaymentMethod.COD);
    }

    public Order(
            String orderCode,
            User user,
            String recipientName,
            String phone,
            String shippingAddress,
            String note,
            PaymentMethod paymentMethod
    ) {
        this.orderCode = orderCode;
        this.user = user;
        this.status = OrderStatus.PENDING;
        this.totalQuantity = 0;
        this.totalAmount = BigDecimal.ZERO.setScale(2);
        this.recipientName = recipientName;
        this.phone = phone;
        this.shippingAddress = shippingAddress;
        this.note = note;
        this.paymentMethod = paymentMethod == null ? PaymentMethod.COD : paymentMethod;
    }

    public void addItem(Product product, int quantity) {
        OrderItem item = new OrderItem(
                this,
                product,
                product.getName(),
                product.getSku(),
                product.getThumbnailUrl(),
                product.getPrice(),
                quantity
        );
        items.add(item);
        totalQuantity = Math.addExact(totalQuantity, quantity);
        totalAmount = totalAmount.add(item.getSubtotal());
    }

    public void confirm(Instant time) {
        status = OrderStatus.CONFIRMED;
        confirmedAt = time;
    }

    public void markPendingPayment() {
        if (status == OrderStatus.PENDING || status == OrderStatus.PENDING_PAYMENT) {
            status = OrderStatus.PENDING_PAYMENT;
        }
    }

    public void markPaid() {
        if (status == OrderStatus.PENDING
                || status == OrderStatus.PENDING_PAYMENT
                || status == OrderStatus.PAID) {
            status = OrderStatus.PAID;
        }
    }

    public void markPaymentFailed() {
        if (status == OrderStatus.PENDING_PAYMENT) {
            status = OrderStatus.PENDING;
        }
    }

    public void startShipping(Instant time) {
        status = OrderStatus.SHIPPING;
        shippedAt = time;
    }

    public void markDelivered(Instant time) {
        status = OrderStatus.DELIVERED;
        deliveredAt = time;
    }

    public void complete(Instant time) {
        status = OrderStatus.COMPLETED;
        completedAt = time;
    }

    public void cancel(Instant time) {
        status = OrderStatus.CANCELLED;
        cancelledAt = time;
    }

    public void addStatusHistory(
            OrderStatus oldStatus,
            OrderStatus newStatus,
            String note,
            User changedBy,
            Instant time
    ) {
        statusHistory.add(new OrderStatusHistory(
                this,
                oldStatus,
                newStatus,
                note,
                changedBy,
                time
        ));
    }

    public Long getId() {
        return id;
    }

    public String getOrderCode() {
        return orderCode;
    }

    public User getUser() {
        return user;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public int getTotalQuantity() {
        return totalQuantity;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getRecipientName() {
        return recipientName == null || recipientName.isBlank()
                ? user.getFullName()
                : recipientName;
    }

    public String getPhone() {
        return phone == null || phone.isBlank() ? user.getPhone() : phone;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public String getNote() {
        return note;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod == null ? PaymentMethod.COD : paymentMethod;
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }

    public Instant getShippedAt() {
        return shippedAt;
    }

    public Instant getDeliveredAt() {
        return deliveredAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
