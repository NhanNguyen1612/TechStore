package com.techstore.admin.service.impl;

import com.techstore.admin.dto.response.AdminResponses;
import com.techstore.admin.exception.AdminErrorCode;
import com.techstore.admin.exception.AdminException;
import com.techstore.admin.repository.AdminDashboardRepository;
import com.techstore.admin.service.AdminDashboardService;
import com.techstore.auth.entity.Role;
import com.techstore.dashboard.dto.response.TopCustomerResponse;
import com.techstore.dashboard.dto.response.TopProductResponse;
import com.techstore.dashboard.repository.DashboardQueryRepository;
import com.techstore.order.entity.OrderStatus;
import com.techstore.payment.entity.PaymentStatus;
import com.techstore.product.entity.Product;
import com.techstore.review.entity.ReviewStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final AdminDashboardRepository repository;
    private final DashboardQueryRepository dashboardRepository;
    private final Clock clock;

    public AdminDashboardServiceImpl(
            AdminDashboardRepository repository,
            DashboardQueryRepository dashboardRepository,
            Clock clock
    ) {
        this.repository = repository;
        this.dashboardRepository = dashboardRepository;
        this.clock = clock;
    }

    @Override
    public AdminResponses.Overview overview() {
        LocalDate today = LocalDate.now(clock);
        DateRange todayRange = range(today, today);
        DateRange monthRange = range(today.withDayOfMonth(1), today);
        BigDecimal totalRevenue = revenue(null, null);
        long totalOrders = repository.orders(null, null, null);
        long cancelled = repository.orders(OrderStatus.CANCELLED, null, null);
        long momo = repository.paymentCount(null);
        long momoPaid = repository.paymentCount(PaymentStatus.PAID);
        return new AdminResponses.Overview(
                totalRevenue,
                revenue(todayRange.from(), todayRange.to()),
                revenue(monthRange.from(), monthRange.to()),
                totalOrders,
                repository.users(),
                repository.products(null),
                repository.ordersInStatuses(List.of(
                        OrderStatus.PAID,
                        OrderStatus.CONFIRMED,
                        OrderStatus.SHIPPING,
                        OrderStatus.DELIVERED,
                        OrderStatus.COMPLETED
                )),
                repository.ordersInStatuses(List.of(
                        OrderStatus.PENDING,
                        OrderStatus.PENDING_PAYMENT
                )),
                cancelled,
                percentage(cancelled, totalOrders),
                percentage(momoPaid, momo)
        );
    }

    @Override
    public AdminResponses.Revenue revenue(
            LocalDate from, LocalDate to, String type
    ) {
        LocalDate today = LocalDate.now(clock);
        LocalDate effectiveTo = to == null ? today : to;
        LocalDate effectiveFrom = from == null ? effectiveTo.minusDays(29) : from;
        DateRange range = range(effectiveFrom, effectiveTo);
        String grouping = normalizeType(type);
        Map<String, MutablePoint> points = new LinkedHashMap<>();
        repository.momoRevenueEvents(range.from(), range.to()).forEach(row ->
                addRevenue(points, (Instant) row[0], BigDecimal.valueOf(
                        ((Number) row[1]).longValue()
                ), grouping)
        );
        repository.codRevenueEvents(range.from(), range.to()).forEach(row ->
                addRevenue(points, (Instant) row[0], (BigDecimal) row[1], grouping)
        );
        repository.orderEvents(range.from(), range.to()).forEach(row ->
                addOrder(points, (Instant) row[0], grouping)
        );
        List<AdminResponses.TimeSeriesPoint> series = points.entrySet().stream()
                .map(entry -> new AdminResponses.TimeSeriesPoint(
                        entry.getKey(),
                        money(entry.getValue().revenue),
                        entry.getValue().orders
                ))
                .toList();
        BigDecimal momo = dashboardRepository.momoRevenue(range.from(), range.to());
        BigDecimal cod = dashboardRepository.codRevenue(range.from(), range.to());
        return new AdminResponses.Revenue(
                momo.add(cod),
                momo,
                cod,
                series,
                effectiveFrom,
                effectiveTo,
                grouping
        );
    }

    @Override
    public AdminResponses.OrdersAnalytics orders(LocalDate from, LocalDate to) {
        DateRange range = optionalRange(from, to);
        List<AdminResponses.CountByLabel> byStatus = Arrays.stream(OrderStatus.values())
                .map(status -> new AdminResponses.CountByLabel(
                        status.name(),
                        repository.orders(status, range.from(), range.to())
                ))
                .toList();
        Map<String, MutablePoint> monthly = new LinkedHashMap<>();
        repository.orderEvents(range.from(), range.to()).forEach(row ->
                addOrder(monthly, (Instant) row[0], "MONTHLY")
        );
        return new AdminResponses.OrdersAnalytics(
                repository.orders(null, range.from(), range.to()),
                repository.orders(
                        null,
                        startOfDay(LocalDate.now(clock)),
                        startOfDay(LocalDate.now(clock).plusDays(1))
                ),
                repository.orders(OrderStatus.SHIPPING, range.from(), range.to()),
                repository.orders(OrderStatus.COMPLETED, range.from(), range.to()),
                repository.orders(OrderStatus.CANCELLED, range.from(), range.to()),
                byStatus,
                monthly.entrySet().stream().map(entry ->
                        new AdminResponses.TimeSeriesPoint(
                                entry.getKey(),
                                BigDecimal.ZERO.setScale(2),
                                entry.getValue().orders
                        )
                ).toList()
        );
    }

    @Override
    public AdminResponses.ProductsAnalytics products(
            int limit, int lowStockThreshold
    ) {
        int safeLimit = limit(limit);
        List<AdminResponses.ProductMetric> bestSellers = repository
                .productsBySold(safeLimit)
                .stream()
                .map(product -> metric(product, BigDecimal.ZERO))
                .toList();
        List<AdminResponses.ProductMetric> lowStock = repository
                .lowStock(threshold(lowStockThreshold), false, safeLimit)
                .stream()
                .map(product -> metric(product, BigDecimal.ZERO))
                .toList();
        List<AdminResponses.ProductMetric> outOfStock = repository
                .lowStock(threshold(lowStockThreshold), true, safeLimit)
                .stream()
                .map(product -> metric(product, BigDecimal.ZERO))
                .toList();
        List<AdminResponses.ProductMetric> revenue = dashboardRepository
                .topProducts(null, null, safeLimit)
                .stream()
                .map(this::metric)
                .toList();
        return new AdminResponses.ProductsAnalytics(
                repository.products(true),
                repository.products(false),
                bestSellers,
                lowStock,
                outOfStock,
                revenue
        );
    }

    @Override
    public AdminResponses.CustomersAnalytics customers(int limit) {
        int safeLimit = limit(limit);
        LocalDate today = LocalDate.now(clock);
        List<TopCustomerResponse> values = dashboardRepository
                .topCustomers(null, null, Math.max(safeLimit, 50));
        List<AdminResponses.CustomerMetric> topByOrders = values.stream()
                .sorted((left, right) -> Long.compare(
                        right.totalOrders(), left.totalOrders()
                ))
                .limit(safeLimit)
                .map(this::customer)
                .toList();
        List<AdminResponses.CustomerMetric> topBySpending = values.stream()
                .limit(safeLimit)
                .map(this::customer)
                .toList();
        return new AdminResponses.CustomersAnalytics(
                repository.users(Role.ROLE_CUSTOMER, null, null),
                repository.users(
                        Role.ROLE_CUSTOMER,
                        startOfDay(today),
                        startOfDay(today.plusDays(1))
                ),
                repository.users(
                        Role.ROLE_CUSTOMER,
                        startOfDay(today.withDayOfMonth(1)),
                        startOfDay(today.plusDays(1))
                ),
                topByOrders,
                topBySpending
        );
    }

    @Override
    public AdminResponses.PaymentsAnalytics payments() {
        long total = repository.paymentCount(null);
        long paid = repository.paymentCount(PaymentStatus.PAID);
        return new AdminResponses.PaymentsAnalytics(
                total + repository.codOrders(),
                total,
                repository.codOrders(),
                paid,
                repository.paymentCount(PaymentStatus.FAILED),
                repository.paymentCount(PaymentStatus.PENDING),
                percentage(paid, total)
        );
    }

    @Override
    public AdminResponses.ReviewsAnalytics reviews() {
        Map<Integer, Long> counts = new LinkedHashMap<>();
        repository.reviewsByRating().forEach(row ->
                counts.put(((Number) row[0]).intValue(), ((Number) row[1]).longValue())
        );
        List<AdminResponses.CountByLabel> byRating = new ArrayList<>();
        for (int rating = 1; rating <= 5; rating++) {
            byRating.add(new AdminResponses.CountByLabel(
                    String.valueOf(rating),
                    counts.getOrDefault(rating, 0L)
            ));
        }
        return new AdminResponses.ReviewsAnalytics(
                repository.reviewCount(null),
                repository.reviewCount(ReviewStatus.PENDING),
                repository.reviewCount(ReviewStatus.APPROVED),
                repository.reviewCount(ReviewStatus.HIDDEN),
                repository.averageRating().setScale(2, RoundingMode.HALF_UP),
                byRating
        );
    }

    @Override
    public AdminResponses.InventoryAnalytics inventory(int lowStockThreshold) {
        int threshold = threshold(lowStockThreshold);
        return new AdminResponses.InventoryAnalytics(
                repository.totalStock(),
                repository.lowStockCount(threshold),
                repository.outOfStockCount(),
                money(repository.inventoryValue())
        );
    }

    private BigDecimal revenue(Instant from, Instant to) {
        return dashboardRepository.momoRevenue(from, to)
                .add(dashboardRepository.codRevenue(from, to));
    }

    private void addRevenue(
            Map<String, MutablePoint> points,
            Instant instant,
            BigDecimal amount,
            String type
    ) {
        String key = period(instant, type);
        points.computeIfAbsent(key, ignored -> new MutablePoint()).revenue =
                points.get(key).revenue.add(amount);
    }

    private void addOrder(
            Map<String, MutablePoint> points,
            Instant instant,
            String type
    ) {
        String key = period(instant, type);
        points.computeIfAbsent(key, ignored -> new MutablePoint()).orders++;
    }

    private String period(Instant instant, String type) {
        LocalDate date = instant.atZone(ZoneOffset.UTC).toLocalDate();
        return switch (type) {
            case "YEARLY" -> DateTimeFormatter.ofPattern("yyyy").format(date);
            case "MONTHLY" -> DateTimeFormatter.ofPattern("yyyy-MM").format(date);
            default -> DateTimeFormatter.ISO_LOCAL_DATE.format(date);
        };
    }

    private String normalizeType(String type) {
        String value = type == null ? "DAILY" : type.toUpperCase();
        if (!SetHolder.TYPES.contains(value)) {
            throw new AdminException(
                    AdminErrorCode.INVALID_DATE_RANGE,
                    "Type must be DAILY, MONTHLY or YEARLY"
            );
        }
        return value;
    }

    private DateRange optionalRange(LocalDate from, LocalDate to) {
        if (from == null && to == null) {
            return new DateRange(null, null);
        }
        LocalDate effectiveTo = to == null ? LocalDate.now(clock) : to;
        LocalDate effectiveFrom = from == null ? effectiveTo.minusDays(29) : from;
        return range(effectiveFrom, effectiveTo);
    }

    private DateRange range(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new AdminException(
                    AdminErrorCode.INVALID_DATE_RANGE,
                    "From date must not be after to date"
            );
        }
        return new DateRange(startOfDay(from), startOfDay(to.plusDays(1)));
    }

    private Instant startOfDay(LocalDate date) {
        return date.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private int limit(int limit) {
        return Math.min(Math.max(limit, 1), 50);
    }

    private int threshold(int value) {
        return Math.max(value, 0);
    }

    private BigDecimal percentage(long part, long total) {
        if (total == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return BigDecimal.valueOf(part)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    private AdminResponses.ProductMetric metric(
            Product product, BigDecimal revenue
    ) {
        return new AdminResponses.ProductMetric(
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getStockQuantity(),
                product.getSoldCount(),
                money(revenue)
        );
    }

    private AdminResponses.ProductMetric metric(TopProductResponse value) {
        return new AdminResponses.ProductMetric(
                value.productId(),
                value.productName(),
                value.sku(),
                0,
                value.quantitySold(),
                value.revenue()
        );
    }

    private AdminResponses.CustomerMetric customer(TopCustomerResponse value) {
        return new AdminResponses.CustomerMetric(
                value.customerId(),
                value.fullName(),
                value.email(),
                value.totalOrders(),
                value.totalSpent()
        );
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private record DateRange(Instant from, Instant to) {
    }

    private static class MutablePoint {
        private BigDecimal revenue = BigDecimal.ZERO;
        private long orders;
    }

    private static final class SetHolder {
        private static final java.util.Set<String> TYPES =
                java.util.Set.of("DAILY", "MONTHLY", "YEARLY");

        private SetHolder() {
        }
    }
}
