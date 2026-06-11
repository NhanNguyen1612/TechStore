package com.techstore.order.dto.response;

import com.techstore.order.entity.OrderStatus;
import com.techstore.order.entity.OrderPaymentStatus;
import com.techstore.order.entity.PaymentMethod;
import java.math.BigDecimal;
import java.time.Instant;

public record OrderSummaryResponse(
        Long id,
        String orderCode,
        Long customerId,
        String customerName,
        OrderStatus status,
        PaymentMethod paymentMethod,
        OrderPaymentStatus paymentStatus,
        int totalQuantity,
        BigDecimal totalAmount,
        Instant createdAt,
        Instant updatedAt
) {
}
