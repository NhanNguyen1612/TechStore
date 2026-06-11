package com.techstore.dashboard.dto.response;

import java.time.LocalDate;
import java.util.List;

public record DashboardProductsResponse(
        long totalProducts,
        long outOfStockProducts,
        List<TopProductResponse> topSellingProducts,
        LocalDate from,
        LocalDate to
) {
}
