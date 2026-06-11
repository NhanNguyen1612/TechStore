package com.techstore.chat.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.techstore.chat.entity.MessageType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SendMessageRequest(
        Long conversationId,
        Long recipientId,
        @NotNull(message = "Message type is required")
        MessageType type,
        @Size(max = 2048, message = "Message content must not exceed 2048 characters")
        String content
) {

    @AssertTrue(message = "Provide exactly one of conversationId or recipientId")
    @JsonIgnore
    public boolean isTargetValid() {
        return (conversationId == null) != (recipientId == null);
    }

    @AssertTrue(message = "Text cannot be blank and image content must be a valid URL")
    @JsonIgnore
    public boolean isContentValid() {
        if (type == null || content == null || content.isBlank()) {
            return false;
        }
        if (type == MessageType.TEXT) {
            return content.trim().length() <= 2000;
        }
        return content.startsWith("https://") || content.startsWith("http://");
    }
}
