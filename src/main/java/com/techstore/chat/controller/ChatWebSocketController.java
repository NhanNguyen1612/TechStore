package com.techstore.chat.controller;

import com.techstore.auth.dto.response.ApiResponse;
import com.techstore.auth.exception.ErrorCode;
import com.techstore.auth.security.AuthUserPrincipal;
import com.techstore.chat.dto.request.SendMessageRequest;
import com.techstore.chat.exception.ChatException;
import com.techstore.chat.service.ChatService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

@Controller
public class ChatWebSocketController {

    private final ChatService chatService;

    public ChatWebSocketController(ChatService chatService) {
        this.chatService = chatService;
    }

    @MessageMapping("/send")
    public void sendMessage(
            @Valid @Payload SendMessageRequest request,
            Principal principal
    ) {
        chatService.sendMessage(userId(principal), request);
    }

    @MessageMapping("/private")
    public void sendPrivateMessage(
            @Valid @Payload SendMessageRequest request,
            Principal principal
    ) {
        chatService.sendMessage(userId(principal), request);
    }

    @MessageExceptionHandler(ChatException.class)
    @SendToUser("/queue/chat/errors")
    public ApiResponse<Void> handleChatException(ChatException exception) {
        return ApiResponse.error(
                exception.getErrorCode().name(),
                exception.getMessage(),
                null
        );
    }

    @MessageExceptionHandler(MethodArgumentNotValidException.class)
    @SendToUser("/queue/chat/errors")
    public ApiResponse<Void> handleValidation() {
        ErrorCode error = ErrorCode.VALIDATION_ERROR;
        return ApiResponse.error(error.name(), error.getMessage(), null);
    }

    private Long userId(Principal principal) {
        if (!(principal instanceof Authentication authentication)
                || !(authentication.getPrincipal()
                instanceof AuthUserPrincipal userPrincipal)) {
            throw new BadCredentialsException(
                    "Authenticated WebSocket principal is required"
            );
        }
        return userPrincipal.getId();
    }
}
