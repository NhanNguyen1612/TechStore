package com.techstore.dashboard.service;

import com.techstore.dashboard.dto.response.DashboardCustomersResponse;
import com.techstore.dashboard.dto.response.DashboardOrdersResponse;
import com.techstore.dashboard.dto.response.DashboardOverviewResponse;
import com.techstore.dashboard.dto.response.DashboardProductsResponse;
import com.techstore.dashboard.dto.response.DashboardRevenueResponse;
import java.time.LocalDate;

public interface DashboardService {

    DashboardOverviewResponse getOverview(LocalDate from, LocalDate to);

    DashboardRevenueResponse getRevenue(LocalDate from, LocalDate to);

    DashboardOrdersResponse getOrders(LocalDate from, LocalDate to);

    DashboardProductsResponse getProducts(
            LocalDate from,
            LocalDate to,
            int limit
    );

    DashboardCustomersResponse getCustomers(
            LocalDate from,
            LocalDate to,
            int limit
    );
}
