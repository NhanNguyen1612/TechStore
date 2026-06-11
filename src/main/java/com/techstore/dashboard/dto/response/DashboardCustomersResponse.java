package com.techstore.dashboard.dto.response;

import java.time.LocalDate;
import java.util.List;

public record DashboardCustomersResponse(
        long totalCustomers,
        long newCustomers,
        List<TopCustomerResponse> topCustomers,
        LocalDate from,
        LocalDate to
) {
}
