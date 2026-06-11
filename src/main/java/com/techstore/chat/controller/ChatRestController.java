package com.techstore.chat.controller;

import com.techstore.auth.dto.response.ApiResponse;
import com.techstore.auth.security.AuthUserPrincipal;
import com.techstore.category.dto.response.PageResponse;
import com.techstore.chat.dto.response.ConversationResponse;
import com.techstore.chat.dto.response.ChatContactResponse;
import com.techstore.chat.dto.response.MessageResponse;
import com.techstore.chat.dto.response.ReadReceiptResponse;
import com.techstore.chat.service.ChatService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/api")
public class ChatRestController {

    private final ChatService chatService;

    public ChatRestController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/chat/contacts")
    public ApiResponse<PageResponse<ChatContactResponse>> getContacts(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @RequestParam(defaultValue = "")
            @Size(max = 100, message = "Search must not exceed 100 characters")
            String search,
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "Page must be non-negative")
            int page,
            @RequestParam(defaultValue = "50")
            @Min(value = 1, message = "Size must be at least 1")
            @Max(value = 100, message = "Size must not exceed 100")
            int size
    ) {
        return ApiResponse.success(
                "Chat contacts retrieved",
                chatService.getContacts(
                        principal.getId(),
                        search,
                        page,
                        size
                )
        );
    }

    @GetMapping("/conversations")
    public ApiResponse<PageResponse<ConversationResponse>> getConversations(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "Page must be non-negative")
            int page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "Size must be at least 1")
            @Max(value = 100, message = "Size must not exceed 100")
            int size
    ) {
        return ApiResponse.success(
                "Conversations retrieved",
                chatService.getConversations(principal.getId(), page, size)
        );
    }

    @GetMapping("/messages/{conversationId}")
    public ApiResponse<PageResponse<MessageResponse>> getMessages(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @PathVariable Long conversationId,
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "Page must be non-negative")
            int page,
            @RequestParam(defaultValue = "50")
            @Min(value = 1, message = "Size must be at least 1")
            @Max(value = 100, message = "Size must not exceed 100")
            int size
    ) {
        return ApiResponse.success(
                "Messages retrieved",
                chatService.getMessages(
                        principal.getId(),
                        conversationId,
                        page,
                        size
                )
        );
    }

    @PutMapping("/messages/{conversationId}/read")
    public ApiResponse<ReadReceiptResponse> markAsRead(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @PathVariable Long conversationId
    ) {
        return ApiResponse.success(
                "Messages marked as read",
                chatService.markAsRead(principal.getId(), conversationId)
        );
    }

    @PostMapping(
            value = "/messages/image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ApiResponse<MessageResponse>> sendImage(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @RequestParam(required = false) Long conversationId,
            @RequestParam(required = false) Long recipientId,
            @RequestPart("image") MultipartFile image
    ) {
        MessageResponse response = chatService.sendImage(
                principal.getId(),
                conversationId,
                recipientId,
                image
        );
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Image message sent", response));
    }
}
