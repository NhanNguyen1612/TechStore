package com.techstore.dashboard.controller;

import com.techstore.auth.dto.response.ApiResponse;
import com.techstore.dashboard.dto.response.DashboardCustomersResponse;
import com.techstore.dashboard.dto.response.DashboardOrdersResponse;
import com.techstore.dashboard.dto.response.DashboardOverviewResponse;
import com.techstore.dashboard.dto.response.DashboardProductsResponse;
import com.techstore.dashboard.dto.response.DashboardRevenueResponse;
import com.techstore.dashboard.service.DashboardService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@PreAuthorize("hasRole('ADMIN')")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/overview")
    public ApiResponse<DashboardOverviewResponse> getOverview(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to
    ) {
        return ApiResponse.success(
                "Dashboard overview retrieved",
                dashboardService.getOverview(from, to)
        );
    }

    @GetMapping("/revenue")
    public ApiResponse<DashboardRevenueResponse> getRevenue(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to
    ) {
        return ApiResponse.success(
                "Dashboard revenue retrieved",
                dashboardService.getRevenue(from, to)
        );
    }

    @GetMapping("/orders")
    public ApiResponse<DashboardOrdersResponse> getOrders(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to
    ) {
        return ApiResponse.success(
                "Dashboard order statistics retrieved",
                dashboardService.getOrders(from, to)
        );
    }

    @GetMapping("/products")
    public ApiResponse<DashboardProductsResponse> getProducts(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to,
            @RequestParam(defaultValue = "10")
            int limit
    ) {
        return ApiResponse.success(
                "Dashboard product statistics retrieved",
                dashboardService.getProducts(from, to, limit)
        );
    }

    @GetMapping("/customers")
    public ApiResponse<DashboardCustomersResponse> getCustomers(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to,
            @RequestParam(defaultValue = "10")
            int limit
    ) {
        return ApiResponse.success(
                "Dashboard customer statistics retrieved",
                dashboardService.getCustomers(from, to, limit)
        );
    }
}
