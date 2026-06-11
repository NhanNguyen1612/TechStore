package com.techstore.admin.dto.response;

import com.techstore.admin.entity.CouponType;
import com.techstore.auth.entity.Role;
import com.techstore.chat.entity.MessageType;
import com.techstore.order.entity.OrderStatus;
import com.techstore.payment.entity.PaymentStatus;
import com.techstore.review.entity.ReviewStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;

public final class AdminResponses {

    private AdminResponses() {
    }

    public record PageData<T>(
            List<T> content,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {
        public static <T> PageData<T> from(Page<T> page) {
            return new PageData<>(
                    page.getContent(),
                    page.getNumber(),
                    page.getSize(),
                    page.getTotalElements(),
                    page.getTotalPages()
            );
        }
    }

    public record UserData(
            Long id,
            String email,
            String fullName,
            String phone,
            Role role,
            boolean enabled,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record ProductData(
            Long id,
            String name,
            String slug,
            String sku,
            String description,
            BigDecimal price,
            int stockQuantity,
            long soldCount,
            String thumbnailUrl,
            Long categoryId,
            String categoryName,
            Long brandId,
            String brandName,
            boolean active,
            List<ImageData> images,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record ImageData(Long id, String url, int sortOrder, boolean primary) {
    }

    public record TaxonomyData(
            Long id,
            String name,
            String slug,
            String description,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record OrderData(
            Long id,
            String orderCode,
            Long customerId,
            String customerName,
            String customerEmail,
            OrderStatus status,
            String paymentMethod,
            int totalQuantity,
            BigDecimal totalAmount,
            List<OrderItemData> items,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record OrderItemData(
            Long id,
            Long productId,
            String productName,
            String sku,
            BigDecimal unitPrice,
            int quantity,
            BigDecimal subtotal
    ) {
    }

    public record PaymentData(
            Long id,
            Long orderId,
            String orderCode,
            String provider,
            PaymentStatus status,
            long amount,
            String requestId,
            String momoOrderId,
            Long transactionId,
            Integer resultCode,
            String message,
            Instant paidAt,
            Instant createdAt,
            List<TransactionData> transactions,
            List<CallbackData> callbacks
    ) {
    }

    public record TransactionData(
            Long id,
            String type,
            String requestId,
            String momoOrderId,
            Long transactionId,
            long amount,
            Integer resultCode,
            String message,
            Instant createdAt
    ) {
    }

    public record CallbackData(
            Long id,
            String type,
            String requestId,
            String momoOrderId,
            Long transactionId,
            Integer resultCode,
            boolean signatureValid,
            boolean processed,
            String processingMessage,
            Instant receivedAt
    ) {
    }

    public record CouponData(
            Long id,
            String code,
            String name,
            CouponType type,
            BigDecimal value,
            BigDecimal minimumOrderAmount,
            Integer usageLimit,
            int usedCount,
            Instant startsAt,
            Instant endsAt,
            boolean active,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record ReviewData(
            Long id,
            Long productId,
            String productName,
            Long userId,
            String userName,
            int rating,
            String comment,
            ReviewStatus status,
            List<String> images,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record ConversationData(
            Long id,
            UserData participantOne,
            UserData participantTwo,
            UserData assignedStaff,
            boolean closed,
            Instant closedAt,
            Instant lastMessageAt,
            Instant createdAt
    ) {
    }

    public record MessageData(
            Long id,
            Long senderId,
            String senderName,
            MessageType type,
            String content,
            Instant readAt,
            Instant createdAt
    ) {
    }

    public record NotificationData(
            Long id,
            String title,
            String content,
            Role targetRole,
            Long createdBy,
            String createdByName,
            Instant createdAt
    ) {
    }

    public record Overview(
            BigDecimal totalRevenue,
            BigDecimal todayRevenue,
            BigDecimal monthRevenue,
            long totalOrders,
            long totalUsers,
            long totalProducts,
            long paidOrders,
            long unpaidOrders,
            long cancelledOrders,
            BigDecimal cancellationRate,
            BigDecimal momoSuccessRate
    ) {
    }

    public record TimeSeriesPoint(String period, BigDecimal revenue, long orders) {
    }

    public record Revenue(
            BigDecimal totalRevenue,
            BigDecimal momoRevenue,
            BigDecimal codRevenue,
            List<TimeSeriesPoint> series,
            LocalDate fromDate,
            LocalDate toDate,
            String type
    ) {
    }

    public record CountByLabel(String label, long count) {
    }

    public record OrdersAnalytics(
            long totalOrders,
            long newToday,
            long shipping,
            long completed,
            long cancelled,
            List<CountByLabel> byStatus,
            List<TimeSeriesPoint> monthly
    ) {
    }

    public record ProductMetric(
            Long productId,
            String name,
            String sku,
            int stock,
            long sold,
            BigDecimal revenue
    ) {
    }

    public record ProductsAnalytics(
            long activeProducts,
            long inactiveProducts,
            List<ProductMetric> bestSellers,
            List<ProductMetric> lowStock,
            List<ProductMetric> outOfStock,
            List<ProductMetric> revenueByProduct
    ) {
    }

    public record CustomerMetric(
            Long customerId,
            String name,
            String email,
            long orders,
            BigDecimal spending
    ) {
    }

    public record CustomersAnalytics(
            long totalCustomers,
            long newToday,
            long newThisMonth,
            List<CustomerMetric> topByOrders,
            List<CustomerMetric> topBySpending
    ) {
    }

    public record PaymentsAnalytics(
            long totalTransactions,
            long momoTransactions,
            long codOrders,
            long paid,
            long failed,
            long pending,
            BigDecimal momoSuccessRate
    ) {
    }

    public record ReviewsAnalytics(
            long totalReviews,
            long pending,
            long approved,
            long hidden,
            BigDecimal averageRating,
            List<CountByLabel> byRating
    ) {
    }

    public record InventoryAnalytics(
            long totalStock,
            long lowStockProducts,
            long outOfStockProducts,
            BigDecimal inventoryValue
    ) {
    }
}
