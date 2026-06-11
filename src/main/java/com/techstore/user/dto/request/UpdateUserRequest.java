package com.techstore.user.dto.request;

import com.techstore.auth.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @NotBlank(message = "Full name is required")
        @Size(min = 2, max = 100, message = "Full name must contain 2 to 100 characters")
        String fullName,

        @Pattern(
                regexp = "^$|^\\+?[0-9]{8,15}$",
                message = "Phone must contain 8 to 15 digits and may start with +"
        )
        String phone,

        @NotNull(message = "Role is required")
        Role role,

        @NotNull(message = "Enabled status is required")
        Boolean enabled
) {
}
