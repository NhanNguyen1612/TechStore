package com.techstore.order.dto.response;

import com.techstore.order.entity.OrderStatus;
import java.time.Instant;

public record OrderTimelineResponse(
        OrderStatus status,
        String title,
        String description,
        String note,
        Long changedBy,
        String changedByName,
        Instant time
) {
}
