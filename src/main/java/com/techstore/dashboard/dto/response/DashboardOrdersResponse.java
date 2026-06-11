package com.techstore.dashboard.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DashboardOrdersResponse(
        long totalOrders,
        long cancelledOrders,
        BigDecimal cancellationRate,
        List<OrderStatusStatisticResponse> byStatus,
        LocalDate from,
        LocalDate to
) {
}
