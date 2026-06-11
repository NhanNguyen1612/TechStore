package com.techstore.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "Current password is required")
        @Size(max = 72, message = "Current password must not exceed 72 characters")
        String currentPassword,

        @NotBlank(message = "New password is required")
        @Size(min = 8, max = 72, message = "New password must contain 8 to 72 characters")
        String newPassword,

        @NotBlank(message = "Password confirmation is required")
        @Size(max = 72, message = "Password confirmation must not exceed 72 characters")
        String confirmPassword
) {
}
