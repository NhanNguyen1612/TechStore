package com.techstore.dashboard.service.impl;

import com.techstore.dashboard.dto.response.DashboardCustomersResponse;
import com.techstore.dashboard.dto.response.DashboardOrdersResponse;
import com.techstore.dashboard.dto.response.DashboardOverviewResponse;
import com.techstore.dashboard.dto.response.DashboardProductsResponse;
import com.techstore.dashboard.dto.response.DashboardRevenueResponse;
import com.techstore.dashboard.dto.response.OrderStatusStatisticResponse;
import com.techstore.dashboard.exception.DashboardErrorCode;
import com.techstore.dashboard.exception.DashboardException;
import com.techstore.dashboard.repository.DashboardQueryRepository;
import com.techstore.dashboard.service.DashboardService;
import com.techstore.order.entity.OrderStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final DashboardQueryRepository dashboardRepository;

    public DashboardServiceImpl(
            DashboardQueryRepository dashboardRepository
    ) {
        this.dashboardRepository = dashboardRepository;
    }

    @Override
    public DashboardOverviewResponse getOverview(
            LocalDate from,
            LocalDate to
    ) {
        DateRange range = dateRange(from, to);
        Revenue revenue = revenue(range);
        List<OrderStatusStatisticResponse> statuses = dashboardRepository
                .orderStatistics(range.fromInstant(), range.toExclusive());
        long totalOrders = statuses.stream()
                .mapToLong(OrderStatusStatisticResponse::count)
                .sum();
        long cancelledOrders = count(statuses, OrderStatus.CANCELLED);
        return new DashboardOverviewResponse(
                revenue.total(),
                revenue.momo(),
                revenue.cod(),
                totalOrders,
                dashboardRepository.totalUsers(),
                dashboardRepository.totalCustomers(),
                cancelledOrders,
                percentage(cancelledOrders, totalOrders),
                from,
                to
        );
    }

    @Override
    public DashboardRevenueResponse getRevenue(
            LocalDate from,
            LocalDate to
    ) {
        DateRange range = dateRange(from, to);
        Revenue revenue = revenue(range);
        return new DashboardRevenueResponse(
                revenue.total(),
                revenue.momo(),
                revenue.cod(),
                from,
                to
        );
    }

    @Override
    public DashboardOrdersResponse getOrders(
            LocalDate from,
            LocalDate to
    ) {
        DateRange range = dateRange(from, to);
        List<OrderStatusStatisticResponse> statuses = dashboardRepository
                .orderStatistics(range.fromInstant(), range.toExclusive());
        long totalOrders = statuses.stream()
                .mapToLong(OrderStatusStatisticResponse::count)
                .sum();
        long cancelledOrders = count(statuses, OrderStatus.CANCELLED);
        return new DashboardOrdersResponse(
                totalOrders,
                cancelledOrders,
                percentage(cancelledOrders, totalOrders),
                statuses,
                from,
                to
        );
    }

    @Override
    public DashboardProductsResponse getProducts(
            LocalDate from,
            LocalDate to,
            int limit
    ) {
        validateLimit(limit);
        DateRange range = dateRange(from, to);
        return new DashboardProductsResponse(
                dashboardRepository.totalProducts(),
                dashboardRepository.outOfStockProducts(),
                dashboardRepository.topProducts(
                        range.fromInstant(),
                        range.toExclusive(),
                        limit
                ),
                from,
                to
        );
    }

    @Override
    public DashboardCustomersResponse getCustomers(
            LocalDate from,
            LocalDate to,
            int limit
    ) {
        validateLimit(limit);
        DateRange range = dateRange(from, to);
        return new DashboardCustomersResponse(
                dashboardRepository.totalCustomers(),
                dashboardRepository.newCustomers(
                        range.fromInstant(),
                        range.toExclusive()
                ),
                dashboardRepository.topCustomers(
                        range.fromInstant(),
                        range.toExclusive(),
                        limit
                ),
                from,
                to
        );
    }

    private Revenue revenue(DateRange range) {
        BigDecimal momo = dashboardRepository.momoRevenue(
                range.fromInstant(),
                range.toExclusive()
        );
        BigDecimal cod = dashboardRepository.codRevenue(
                range.fromInstant(),
                range.toExclusive()
        );
        return new Revenue(momo.add(cod), momo, cod);
    }

    private DateRange dateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new DashboardException(
                    DashboardErrorCode.INVALID_DATE_RANGE
            );
        }
        Instant fromInstant = from == null
                ? null
                : from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toExclusive = to == null
                ? null
                : to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        return new DateRange(fromInstant, toExclusive);
    }

    private void validateLimit(int limit) {
        if (limit < 1 || limit > 50) {
            throw new DashboardException(
                    DashboardErrorCode.INVALID_DASHBOARD_QUERY
            );
        }
    }

    private long count(
            List<OrderStatusStatisticResponse> statuses,
            OrderStatus status
    ) {
        return statuses.stream()
                .filter(statistic -> statistic.status() == status)
                .mapToLong(OrderStatusStatisticResponse::count)
                .findFirst()
                .orElse(0L);
    }

    private BigDecimal percentage(long part, long total) {
        if (total == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return BigDecimal.valueOf(part)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    private record DateRange(Instant fromInstant, Instant toExclusive) {
    }

    private record Revenue(
            BigDecimal total,
            BigDecimal momo,
            BigDecimal cod
    ) {
    }
}
