package com.techstore.chat.dto.response;

import java.time.Instant;

public record ConversationResponse(
        Long id,
        ChatParticipantResponse participant,
        MessageResponse lastMessage,
        long unreadCount,
        Instant lastMessageAt,
        Instant createdAt,
        Instant updatedAt
) {
}
