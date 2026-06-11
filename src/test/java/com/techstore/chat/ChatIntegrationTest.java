package com.techstore.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techstore.auth.entity.Role;
import com.techstore.auth.entity.User;
import com.techstore.auth.repository.RefreshTokenRepository;
import com.techstore.auth.repository.UserRepository;
import com.techstore.chat.dto.request.SendMessageRequest;
import com.techstore.chat.dto.response.ChatEventResponse;
import com.techstore.chat.dto.response.MessageResponse;
import com.techstore.chat.entity.MessageType;
import com.techstore.chat.repository.ConversationRepository;
import com.techstore.chat.repository.MessageRepository;
import com.techstore.chat.security.WebSocketJwtChannelInterceptor;
import com.techstore.chat.service.ChatService;
import com.techstore.chat.storage.ChatImageStorage;
import com.techstore.user.repository.UserProfileRepository;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class ChatIntegrationTest {

    private static final String PASSWORD = "Password123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ChatService chatService;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private UserProfileRepository profileRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private WebSocketJwtChannelInterceptor jwtChannelInterceptor;

    @MockitoBean
    private ChatImageStorage imageStorage;

    @MockitoSpyBean
    private SimpMessagingTemplate messagingTemplate;

    @BeforeEach
    void cleanBefore() {
        cleanDatabase();
        clearInvocations(messagingTemplate, imageStorage);
    }

    @AfterEach
    void cleanAfter() {
        cleanDatabase();
    }

    @Test
    void textMessageCreatesConversationPersistsAndPublishesPrivately()
            throws Exception {
        User sender = createUser("sender@chat.test", Role.ROLE_CUSTOMER);
        User recipient = createUser("recipient@chat.test", Role.ROLE_STAFF);
        User outsider = createUser("outsider@chat.test", Role.ROLE_CUSTOMER);

        MessageResponse sent = chatService.sendMessage(
                sender.getId(),
                new SendMessageRequest(
                        null,
                        recipient.getId(),
                        MessageType.TEXT,
                        "Hello support"
                )
        );

        assertThat(sent.mine()).isTrue();
        assertThat(sent.type()).isEqualTo(MessageType.TEXT);
        assertThat(conversationRepository.count()).isEqualTo(1);
        assertThat(messageRepository.count()).isEqualTo(1);
        verify(messagingTemplate).convertAndSendToUser(
                eq(sender.getEmail()),
                eq("/queue/chat/private"),
                any(ChatEventResponse.class)
        );
        verify(messagingTemplate).convertAndSendToUser(
                eq(recipient.getEmail()),
                eq("/queue/chat/private"),
                any(ChatEventResponse.class)
        );

        String recipientToken = login(recipient.getEmail());
        mockMvc.perform(get("/api/conversations")
                        .header("Authorization", bearer(recipientToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id")
                        .value(sent.conversationId()))
                .andExpect(jsonPath("$.data.content[0].participant.id")
                        .value(sender.getId()))
                .andExpect(jsonPath("$.data.content[0].unreadCount").value(1));

        mockMvc.perform(get(
                                "/api/messages/{conversationId}",
                                sent.conversationId()
                        )
                        .header("Authorization", bearer(recipientToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].content")
                        .value("Hello support"))
                .andExpect(jsonPath("$.data.content[0].mine").value(false))
                .andExpect(jsonPath("$.data.content[0].read").value(false));

        String outsiderToken = login(outsider.getEmail());
        mockMvc.perform(get(
                                "/api/messages/{conversationId}",
                                sent.conversationId()
                        )
                        .header("Authorization", bearer(outsiderToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code")
                        .value("CONVERSATION_ACCESS_DENIED"));

        mockMvc.perform(put(
                                "/api/messages/{conversationId}/read",
                                sent.conversationId()
                        )
                        .header("Authorization", bearer(recipientToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.messagesRead").value(1))
                .andExpect(jsonPath("$.data.readAt").isNotEmpty());

        mockMvc.perform(get(
                                "/api/messages/{conversationId}",
                                sent.conversationId()
                        )
                        .header("Authorization", bearer(recipientToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].read").value(true));
    }

    @Test
    void imageUploadCreatesImageMessageAndPublishesIt() throws Exception {
        User sender = createUser("image-sender@chat.test", Role.ROLE_CUSTOMER);
        User recipient = createUser("image-recipient@chat.test", Role.ROLE_ADMIN);
        String token = login(sender.getEmail());
        when(imageStorage.upload(any())).thenReturn(
                new ChatImageStorage.StoredImage(
                        "https://cloudinary.test/chat/message.webp",
                        "chat/message"
                )
        );
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "message.png",
                MediaType.IMAGE_PNG_VALUE,
                new byte[]{1, 2, 3}
        );

        mockMvc.perform(multipart("/api/messages/image")
                        .file(image)
                        .param("recipientId", recipient.getId().toString())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.type").value("IMAGE"))
                .andExpect(jsonPath("$.data.content")
                        .value("https://cloudinary.test/chat/message.webp"));

        assertThat(messageRepository.count()).isEqualTo(1);
        verify(imageStorage).upload(any());
        verify(messagingTemplate).convertAndSendToUser(
                eq(recipient.getEmail()),
                eq("/queue/chat/private"),
                any(ChatEventResponse.class)
        );
    }

    @Test
    void contactsAreSearchableAndFilteredByViewerRole() throws Exception {
        User admin = createUser(
                "admin@chat.test",
                Role.ROLE_ADMIN,
                "Admin Support"
        );
        User staff = createUser(
                "staff@chat.test",
                Role.ROLE_STAFF,
                "Store Support"
        );
        User customer = createUser(
                "customer@chat.test",
                Role.ROLE_CUSTOMER,
                "Customer One"
        );
        User anotherCustomer = createUser(
                "another@chat.test",
                Role.ROLE_CUSTOMER,
                "Customer Two"
        );

        String customerToken = login(customer.getEmail());
        mockMvc.perform(get("/api/chat/contacts")
                        .param("search", "support")
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content[0].fullName")
                        .value("Admin Support"))
                .andExpect(jsonPath("$.data.content[1].fullName")
                        .value("Store Support"));

        mockMvc.perform(get("/api/chat/contacts")
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2));

        String adminToken = login(admin.getEmail());
        mockMvc.perform(get("/api/chat/contacts")
                        .param("search", "customer")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content[0].id")
                        .value(customer.getId()))
                .andExpect(jsonPath("$.data.content[1].id")
                        .value(anotherCustomer.getId()));

        assertThat(staff.getId()).isNotNull();
    }

    @Test
    void websocketConnectRequiresAValidCurrentAccessToken() throws Exception {
        User user = createUser("socket@chat.test", Role.ROLE_CUSTOMER);
        String accessToken = login(user.getEmail());
        StompHeaderAccessor accessor = StompHeaderAccessor.create(
                StompCommand.CONNECT
        );
        accessor.setNativeHeader(
                "Authorization",
                bearer(accessToken)
        );
        accessor.setLeaveMutable(true);
        Message<byte[]> connect = MessageBuilder.createMessage(
                new byte[0],
                accessor.getMessageHeaders()
        );

        jwtChannelInterceptor.preSend(connect, new NoOpMessageChannel());

        StompHeaderAccessor authenticated = MessageHeaderAccessor.getAccessor(
                connect,
                StompHeaderAccessor.class
        );
        assertThat(authenticated).isNotNull();
        assertThat(authenticated.getUser()).isNotNull();
        assertThat(authenticated.getUser().getName()).isEqualTo(user.getEmail());

        StompHeaderAccessor missingToken = StompHeaderAccessor.create(
                StompCommand.CONNECT
        );
        missingToken.setLeaveMutable(true);
        Message<byte[]> invalidConnect = MessageBuilder.createMessage(
                new byte[0],
                missingToken.getMessageHeaders()
        );
        assertThatThrownBy(() -> jwtChannelInterceptor.preSend(
                invalidConnect,
                new NoOpMessageChannel()
        )).isInstanceOf(BadCredentialsException.class);
    }

    private User createUser(String email, Role role) {
        return createUser(email, role, "Chat User");
    }

    private User createUser(String email, Role role, String fullName) {
        return userRepository.saveAndFlush(new User(
                email,
                passwordEncoder.encode(PASSWORD),
                fullName,
                null,
                role
        ));
    }

    private String login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode data = objectMapper.readTree(
                result.getResponse().getContentAsString(StandardCharsets.UTF_8)
        ).path("data");
        return data.path("accessToken").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private void cleanDatabase() {
        messageRepository.deleteAll();
        conversationRepository.deleteAll();
        profileRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    private static class NoOpMessageChannel implements MessageChannel {

        @Override
        public boolean send(Message<?> message) {
            return true;
        }

        @Override
        public boolean send(Message<?> message, long timeout) {
            return true;
        }
    }
}
