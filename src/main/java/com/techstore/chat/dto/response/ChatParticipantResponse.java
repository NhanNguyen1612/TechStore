package com.techstore.chat.dto.response;

import com.techstore.auth.entity.Role;

public record ChatParticipantResponse(
        Long id,
        String fullName,
        Role role,
        String avatarUrl
) {
}
