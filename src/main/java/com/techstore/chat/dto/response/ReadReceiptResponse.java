package com.techstore.chat.dto.response;

import java.time.Instant;

public record ReadReceiptResponse(
        Long conversationId,
        Long readBy,
        int messagesRead,
        Instant readAt
) {
}
