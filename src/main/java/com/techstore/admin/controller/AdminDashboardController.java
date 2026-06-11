package com.techstore.admin.controller;

import com.techstore.admin.dto.response.AdminResponses;
import com.techstore.admin.service.AdminDashboardService;
import com.techstore.auth.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Dashboard")
public class AdminDashboardController {

    private final AdminDashboardService service;

    public AdminDashboardController(AdminDashboardService service) {
        this.service = service;
    }

    @GetMapping("/overview")
    @Operation(summary = "Get admin dashboard overview")
    public ApiResponse<AdminResponses.Overview> overview() {
        return ApiResponse.success("Dashboard overview retrieved", service.overview());
    }

    @GetMapping("/revenue")
    @Operation(summary = "Get revenue grouped by day, month or year")
    public ApiResponse<AdminResponses.Revenue> revenue(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "DAILY") String type
    ) {
        return ApiResponse.success(
                "Revenue analytics retrieved",
                service.revenue(from, to, type)
        );
    }

    @GetMapping("/orders")
    @Operation(summary = "Get order analytics")
    public ApiResponse<AdminResponses.OrdersAnalytics> orders(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ApiResponse.success(
                "Order analytics retrieved",
                service.orders(from, to)
        );
    }

    @GetMapping("/products")
    @Operation(summary = "Get product analytics")
    public ApiResponse<AdminResponses.ProductsAnalytics> products(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "5") int lowStockThreshold
    ) {
        return ApiResponse.success(
                "Product analytics retrieved",
                service.products(limit, lowStockThreshold)
        );
    }

    @GetMapping("/customers")
    @Operation(summary = "Get customer analytics")
    public ApiResponse<AdminResponses.CustomersAnalytics> customers(
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ApiResponse.success(
                "Customer analytics retrieved",
                service.customers(limit)
        );
    }

    @GetMapping("/payments")
    @Operation(summary = "Get payment analytics")
    public ApiResponse<AdminResponses.PaymentsAnalytics> payments() {
        return ApiResponse.success(
                "Payment analytics retrieved",
                service.payments()
        );
    }

    @GetMapping("/reviews")
    @Operation(summary = "Get review analytics")
    public ApiResponse<AdminResponses.ReviewsAnalytics> reviews() {
        return ApiResponse.success(
                "Review analytics retrieved",
                service.reviews()
        );
    }

    @GetMapping("/inventory")
    @Operation(summary = "Get inventory analytics")
    public ApiResponse<AdminResponses.InventoryAnalytics> inventory(
            @RequestParam(defaultValue = "5") int lowStockThreshold
    ) {
        return ApiResponse.success(
                "Inventory analytics retrieved",
                service.inventory(lowStockThreshold)
        );
    }
}
