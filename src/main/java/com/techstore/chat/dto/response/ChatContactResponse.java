package com.techstore.chat.dto.response;

import com.techstore.auth.entity.Role;

public record ChatContactResponse(
        Long id,
        String fullName,
        String email,
        Role role,
        String avatarUrl
) {
}
