package com.techstore.order.dto.response;

import com.techstore.order.entity.OrderStatus;
import com.techstore.order.entity.OrderPaymentStatus;
import com.techstore.order.entity.PaymentMethod;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderDetailResponse(
        Long id,
        String orderCode,
        Long customerId,
        String customerName,
        String customerEmail,
        String recipientName,
        String phone,
        String shippingAddress,
        String note,
        OrderStatus status,
        PaymentMethod paymentMethod,
        OrderPaymentStatus paymentStatus,
        Long transactionId,
        int totalQuantity,
        BigDecimal totalAmount,
        List<OrderItemResponse> items,
        Instant confirmedAt,
        Instant shippedAt,
        Instant deliveredAt,
        Instant cancelledAt,
        Instant completedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
