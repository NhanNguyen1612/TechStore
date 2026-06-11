package com.techstore.order.dto.response;

import com.techstore.order.entity.OrderPaymentStatus;
import com.techstore.order.entity.OrderStatus;
import com.techstore.order.entity.PaymentMethod;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderTrackingResponse(
        Long orderId,
        String orderCode,
        String customerName,
        String phone,
        String shippingAddress,
        String note,
        PaymentMethod paymentMethod,
        OrderPaymentStatus paymentStatus,
        Long transactionId,
        OrderStatus orderStatus,
        BigDecimal totalAmount,
        List<OrderItemResponse> items,
        List<OrderTimelineResponse> timeline,
        String cancellationReason,
        Instant createdAt,
        Instant updatedAt
) {
}
