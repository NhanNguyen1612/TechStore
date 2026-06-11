package com.techstore.user.dto.response;

import com.techstore.auth.entity.Role;
import java.time.Instant;

public record UserResponse(
        Long id,
        String email,
        String fullName,
        String phone,
        String avatarUrl,
        Role role,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt,
        Long avatarCreatedBy,
        Long avatarUpdatedBy
) {
}
