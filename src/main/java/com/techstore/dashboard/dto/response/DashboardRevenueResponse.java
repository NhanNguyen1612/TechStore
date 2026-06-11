package com.techstore.dashboard.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DashboardRevenueResponse(
        BigDecimal totalRevenue,
        BigDecimal momoRevenue,
        BigDecimal codRevenue,
        LocalDate from,
        LocalDate to
) {
}
