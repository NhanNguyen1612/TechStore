package com.techstore.admin.service;

import com.techstore.admin.dto.response.AdminResponses;
import java.time.LocalDate;

public interface AdminDashboardService {

    AdminResponses.Overview overview();

    AdminResponses.Revenue revenue(LocalDate from, LocalDate to, String type);

    AdminResponses.OrdersAnalytics orders(LocalDate from, LocalDate to);

    AdminResponses.ProductsAnalytics products(int limit, int lowStockThreshold);

    AdminResponses.CustomersAnalytics customers(int limit);

    AdminResponses.PaymentsAnalytics payments();

    AdminResponses.ReviewsAnalytics reviews();

    AdminResponses.InventoryAnalytics inventory(int lowStockThreshold);
}
