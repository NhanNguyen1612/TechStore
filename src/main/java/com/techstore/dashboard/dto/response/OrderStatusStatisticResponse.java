package com.techstore.dashboard.dto.response;

import com.techstore.order.entity.OrderStatus;

public record OrderStatusStatisticResponse(
        OrderStatus status,
        long count
) {
}
