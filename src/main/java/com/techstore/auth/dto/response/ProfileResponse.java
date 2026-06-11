package com.techstore.auth.dto.response;

import com.techstore.auth.entity.Role;
import java.time.Instant;

public record ProfileResponse(
        Long id,
        String email,
        String fullName,
        String phone,
        Role role,
        Instant createdAt,
        Instant updatedAt
) {
}
