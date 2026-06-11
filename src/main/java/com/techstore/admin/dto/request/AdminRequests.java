package com.techstore.admin.dto.request;

import com.techstore.admin.entity.CouponType;
import com.techstore.auth.entity.Role;
import com.techstore.payment.entity.PaymentStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

public final class AdminRequests {

    private AdminRequests() {
    }

    public record CreateUser(
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotBlank @Size(min = 2, max = 100) String fullName,
            @Pattern(regexp = "^$|^\\+?[0-9]{8,15}$") String phone,
            @NotNull Role role,
            boolean enabled
    ) {
    }

    public record UpdateUser(
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(min = 2, max = 100) String fullName,
            @Pattern(regexp = "^$|^\\+?[0-9]{8,15}$") String phone
    ) {
    }

    public record ChangeStatus(boolean active) {
    }

    public record ChangeRole(@NotNull Role role) {
    }

    public record ChangePaymentStatus(@NotNull PaymentStatus status) {
    }

    public record UpdateStock(@Min(0) int stockQuantity) {
    }

    public record AssignStaff(@NotNull Long staffId) {
    }

    public record UpsertCoupon(
            @NotBlank @Size(max = 50) String code,
            @NotBlank @Size(max = 150) String name,
            @NotNull CouponType type,
            @NotNull @DecimalMin("0.01") BigDecimal value,
            @NotNull @DecimalMin("0.00") BigDecimal minimumOrderAmount,
            @Min(1) Integer usageLimit,
            @NotNull Instant startsAt,
            @NotNull Instant endsAt
    ) {
    }

    public record CreateNotification(
            @NotBlank @Size(max = 150) String title,
            @NotBlank @Size(max = 4000) String content,
            Role targetRole
    ) {
    }

    public record ProductFilter(
            String search,
            Long categoryId,
            Long brandId,
            Boolean active,
            @Min(0) int page,
            @Min(1) @Max(100) int size
    ) {
    }
}
