package com.techstore.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = "Full name is required")
        @Size(min = 2, max = 100, message = "Full name must contain 2 to 100 characters")
        String fullName,

        @Pattern(
                regexp = "^$|^\\+?[0-9]{8,15}$",
                message = "Phone must contain 8 to 15 digits and may start with +"
        )
        String phone
) {
}
