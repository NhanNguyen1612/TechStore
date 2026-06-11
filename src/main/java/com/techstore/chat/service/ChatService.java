package com.techstore.chat.service;

import com.techstore.category.dto.response.PageResponse;
import com.techstore.chat.dto.request.SendMessageRequest;
import com.techstore.chat.dto.response.ChatContactResponse;
import com.techstore.chat.dto.response.ConversationResponse;
import com.techstore.chat.dto.response.MessageResponse;
import com.techstore.chat.dto.response.ReadReceiptResponse;
import org.springframework.web.multipart.MultipartFile;

public interface ChatService {

    PageResponse<ChatContactResponse> getContacts(
            Long userId,
            String search,
            int page,
            int size
    );

    PageResponse<ConversationResponse> getConversations(
            Long userId,
            int page,
            int size
    );

    PageResponse<MessageResponse> getMessages(
            Long userId,
            Long conversationId,
            int page,
            int size
    );

    MessageResponse sendMessage(Long senderId, SendMessageRequest request);

    MessageResponse sendImage(
            Long senderId,
            Long conversationId,
            Long recipientId,
            MultipartFile image
    );

    ReadReceiptResponse markAsRead(Long userId, Long conversationId);
}
