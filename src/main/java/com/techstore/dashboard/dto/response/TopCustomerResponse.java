package com.techstore.dashboard.dto.response;

import java.math.BigDecimal;

public record TopCustomerResponse(
        Long customerId,
        String fullName,
        String email,
        long totalOrders,
        BigDecimal totalSpent
) {
}
