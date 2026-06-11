package com.techstore.chat.dto.response;

import com.techstore.chat.entity.MessageType;
import java.time.Instant;

public record MessageResponse(
        Long id,
        Long conversationId,
        Long senderId,
        String senderName,
        String senderAvatarUrl,
        MessageType type,
        String content,
        boolean mine,
        boolean read,
        Instant readAt,
        Instant createdAt
) {
}
