package com.techstore.dashboard.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DashboardOverviewResponse(
        BigDecimal totalRevenue,
        BigDecimal momoRevenue,
        BigDecimal codRevenue,
        long totalOrders,
        long totalUsers,
        long totalCustomers,
        long cancelledOrders,
        BigDecimal cancellationRate,
        LocalDate from,
        LocalDate to
) {
}
