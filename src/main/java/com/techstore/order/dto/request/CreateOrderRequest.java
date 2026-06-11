package com.techstore.order.dto.request;

import com.techstore.order.entity.PaymentMethod;
import jakarta.validation.constraints.Size;

public record CreateOrderRequest(
        @Size(max = 100, message = "Recipient name must not exceed 100 characters")
        String recipientName,

        @Size(max = 20, message = "Phone must not exceed 20 characters")
        String phone,

        @Size(max = 500, message = "Shipping address must not exceed 500 characters")
        String shippingAddress,

        @Size(max = 1000, message = "Note must not exceed 1000 characters")
        String note,

        PaymentMethod paymentMethod
) {
}
