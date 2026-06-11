package com.techstore.chat.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatEventResponse(
        EventType eventType,
        MessageResponse message,
        ReadReceiptResponse readReceipt
) {

    public enum EventType {
        MESSAGE,
        READ
    }

    public static ChatEventResponse message(MessageResponse message) {
        return new ChatEventResponse(EventType.MESSAGE, message, null);
    }

    public static ChatEventResponse read(ReadReceiptResponse readReceipt) {
        return new ChatEventResponse(EventType.READ, null, readReceipt);
    }
}
