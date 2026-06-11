package com.techstore.chat.service.impl;

import com.techstore.auth.entity.User;
import com.techstore.auth.entity.Role;
import com.techstore.auth.repository.UserRepository;
import com.techstore.category.dto.response.PageResponse;
import com.techstore.chat.config.ChatImageProperties;
import com.techstore.chat.dto.request.SendMessageRequest;
import com.techstore.chat.dto.response.ChatEventResponse;
import com.techstore.chat.dto.response.ChatContactResponse;
import com.techstore.chat.dto.response.ChatParticipantResponse;
import com.techstore.chat.dto.response.ConversationResponse;
import com.techstore.chat.dto.response.MessageResponse;
import com.techstore.chat.dto.response.ReadReceiptResponse;
import com.techstore.chat.entity.Conversation;
import com.techstore.chat.entity.Message;
import com.techstore.chat.entity.MessageType;
import com.techstore.chat.exception.ChatErrorCode;
import com.techstore.chat.exception.ChatException;
import com.techstore.chat.repository.ConversationRepository;
import com.techstore.chat.repository.ChatContactRepository;
import com.techstore.chat.repository.MessageRepository;
import com.techstore.chat.service.ChatService;
import com.techstore.chat.storage.ChatImageStorage;
import com.techstore.user.entity.UserProfile;
import com.techstore.user.repository.UserProfileRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ChatServiceImpl implements ChatService {

    private static final String PRIVATE_DESTINATION = "/queue/chat/private";
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final ConversationRepository conversationRepository;
    private final ChatContactRepository contactRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;
    private final ChatImageStorage imageStorage;
    private final ChatImageProperties imageProperties;
    private final SimpMessagingTemplate messagingTemplate;
    private final Clock clock;

    public ChatServiceImpl(
            ConversationRepository conversationRepository,
            ChatContactRepository contactRepository,
            MessageRepository messageRepository,
            UserRepository userRepository,
            UserProfileRepository profileRepository,
            ChatImageStorage imageStorage,
            ChatImageProperties imageProperties,
            SimpMessagingTemplate messagingTemplate,
            Clock clock
    ) {
        this.conversationRepository = conversationRepository;
        this.contactRepository = contactRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.imageStorage = imageStorage;
        this.imageProperties = imageProperties;
        this.messagingTemplate = messagingTemplate;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ChatContactResponse> getContacts(
            Long userId,
            String search,
            int page,
            int size
    ) {
        User viewer = findUser(userId);
        String normalizedSearch = Optional.ofNullable(search)
                .orElse("")
                .trim()
                .toLowerCase(Locale.ROOT);
        PageRequest pageable = PageRequest.of(page, size);
        Page<ChatContactResponse> contacts;
        if (viewer.getRole() == Role.ROLE_CUSTOMER) {
            contacts = contactRepository.findAvailableByRole(
                    userId,
                    Set.of(Role.ROLE_ADMIN, Role.ROLE_STAFF),
                    normalizedSearch,
                    pageable
            );
        } else {
            contacts = contactRepository.findAllAvailable(
                    userId,
                    normalizedSearch,
                    pageable
            );
        }
        return PageResponse.from(contacts);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ConversationResponse> getConversations(
            Long userId,
            int page,
            int size
    ) {
        findUser(userId);
        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.desc("lastMessageAt").nullsLast(),
                        Sort.Order.desc("id")
                )
        );
        Page<ConversationResponse> conversations = conversationRepository
                .findAllForUser(userId, pageable)
                .map(conversation -> toConversationResponse(conversation, userId));
        return PageResponse.from(conversations);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<MessageResponse> getMessages(
            Long userId,
            Long conversationId,
            int page,
            int size
    ) {
        Conversation conversation = findAccessibleConversation(
                conversationId,
                userId
        );
        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("id")
                )
        );
        Page<MessageResponse> messages = messageRepository
                .findAllByConversationId(conversation.getId(), pageable)
                .map(message -> toMessageResponse(message, userId));
        return PageResponse.from(messages);
    }

    @Override
    @Transactional
    public MessageResponse sendMessage(
            Long senderId,
            SendMessageRequest request
    ) {
        validateSendRequest(request);
        return saveAndPublish(
                senderId,
                request.conversationId(),
                request.recipientId(),
                request.type(),
                normalizeContent(request.content()),
                null
        );
    }

    @Override
    @Transactional
    public MessageResponse sendImage(
            Long senderId,
            Long conversationId,
            Long recipientId,
            MultipartFile image
    ) {
        validateTarget(conversationId, recipientId);
        validateImage(image);
        ChatImageStorage.StoredImage uploaded = imageStorage.upload(image);
        registerImageRollback(uploaded.publicId());
        return saveAndPublish(
                senderId,
                conversationId,
                recipientId,
                MessageType.IMAGE,
                uploaded.url(),
                uploaded.publicId()
        );
    }

    @Override
    @Transactional
    public ReadReceiptResponse markAsRead(Long userId, Long conversationId) {
        Conversation conversation = findAccessibleConversation(
                conversationId,
                userId
        );
        Instant readAt = clock.instant();
        int messagesRead = messageRepository.markUnreadAsRead(
                conversationId,
                userId,
                readAt
        );
        ReadReceiptResponse receipt = new ReadReceiptResponse(
                conversationId,
                userId,
                messagesRead,
                readAt
        );
        if (messagesRead > 0) {
            publishAfterCommit(
                    conversation,
                    ChatEventResponse.read(receipt)
            );
        }
        return receipt;
    }

    private MessageResponse saveAndPublish(
            Long senderId,
            Long conversationId,
            Long recipientId,
            MessageType type,
            String content,
            String imagePublicId
    ) {
        User sender = findUser(senderId);
        Conversation conversation = resolveConversation(
                sender,
                conversationId,
                recipientId
        );
        Instant sentAt = clock.instant();
        Message message = new Message(
                conversation,
                sender,
                type,
                content,
                imagePublicId
        );
        conversation.touch(sentAt);
        conversationRepository.save(conversation);
        Message saved = messageRepository.saveAndFlush(message);

        MessageResponse senderResponse = toMessageResponse(saved, senderId);
        User recipient = conversation.otherParticipant(senderId);
        MessageResponse recipientResponse = toMessageResponse(
                saved,
                recipient.getId()
        );
        publishAfterCommit(
                sender.getEmail(),
                ChatEventResponse.message(senderResponse)
        );
        publishAfterCommit(
                recipient.getEmail(),
                ChatEventResponse.message(recipientResponse)
        );
        return senderResponse;
    }

    private Conversation resolveConversation(
            User sender,
            Long conversationId,
            Long recipientId
    ) {
        validateTarget(conversationId, recipientId);
        if (conversationId != null) {
            return findAccessibleConversation(conversationId, sender.getId());
        }

        User recipient = findUser(recipientId);
        if (sender.getId().equals(recipient.getId())) {
            throw new ChatException(
                    ChatErrorCode.INVALID_CONVERSATION_PARTICIPANTS
            );
        }
        String participantKey = Conversation.participantKey(
                sender.getId(),
                recipient.getId()
        );
        return conversationRepository.findByParticipantKey(participantKey)
                .orElseGet(() -> conversationRepository.saveAndFlush(
                        new Conversation(sender, recipient)
                ));
    }

    private Conversation findAccessibleConversation(
            Long conversationId,
            Long userId
    ) {
        Conversation conversation = conversationRepository
                .findDetailById(conversationId)
                .orElseThrow(() -> new ChatException(
                        ChatErrorCode.CONVERSATION_NOT_FOUND
                ));
        if (!conversation.hasParticipant(userId)) {
            throw new ChatException(ChatErrorCode.CONVERSATION_ACCESS_DENIED);
        }
        return conversation;
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .filter(User::isEnabled)
                .orElseThrow(() -> new ChatException(
                        ChatErrorCode.USER_NOT_FOUND
                ));
    }

    private ConversationResponse toConversationResponse(
            Conversation conversation,
            Long viewerId
    ) {
        User participant = conversation.otherParticipant(viewerId);
        MessageResponse lastMessage = messageRepository
                .findFirstByConversationIdOrderByCreatedAtDescIdDesc(
                        conversation.getId()
                )
                .map(message -> toMessageResponse(message, viewerId))
                .orElse(null);
        long unreadCount = messageRepository
                .countByConversationIdAndSenderIdNotAndReadAtIsNull(
                        conversation.getId(),
                        viewerId
                );
        return new ConversationResponse(
                conversation.getId(),
                toParticipantResponse(participant),
                lastMessage,
                unreadCount,
                conversation.getLastMessageAt(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt()
        );
    }

    private ChatParticipantResponse toParticipantResponse(User user) {
        return new ChatParticipantResponse(
                user.getId(),
                user.getFullName(),
                user.getRole(),
                avatarUrl(user.getId())
        );
    }

    private MessageResponse toMessageResponse(Message message, Long viewerId) {
        User sender = message.getSender();
        return new MessageResponse(
                message.getId(),
                message.getConversation().getId(),
                sender.getId(),
                sender.getFullName(),
                avatarUrl(sender.getId()),
                message.getType(),
                message.getContent(),
                sender.getId().equals(viewerId),
                message.getReadAt() != null,
                message.getReadAt(),
                message.getCreatedAt()
        );
    }

    private String avatarUrl(Long userId) {
        return profileRepository.findById(userId)
                .map(UserProfile::getAvatarUrl)
                .orElse(null);
    }

    private void validateSendRequest(SendMessageRequest request) {
        if (request == null || request.type() == null) {
            throw new ChatException(ChatErrorCode.INVALID_MESSAGE);
        }
        validateTarget(request.conversationId(), request.recipientId());
        String content = request.content();
        if (content == null || content.isBlank()) {
            throw new ChatException(ChatErrorCode.INVALID_MESSAGE);
        }
        if (request.type() == MessageType.TEXT
                && content.trim().length() > 2000) {
            throw new ChatException(ChatErrorCode.INVALID_MESSAGE);
        }
        if (request.type() == MessageType.IMAGE
                && !isHttpUrl(content.trim())) {
            throw new ChatException(ChatErrorCode.INVALID_MESSAGE);
        }
    }

    private void validateTarget(Long conversationId, Long recipientId) {
        if ((conversationId == null) == (recipientId == null)) {
            throw new ChatException(ChatErrorCode.INVALID_MESSAGE);
        }
    }

    private void validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new ChatException(ChatErrorCode.IMAGE_REQUIRED);
        }
        if (image.getSize() > imageProperties.maxFileSize().toBytes()) {
            throw new ChatException(ChatErrorCode.IMAGE_TOO_LARGE);
        }
        String contentType = Optional.ofNullable(image.getContentType())
                .orElse("")
                .toLowerCase(Locale.ROOT);
        if (!ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new ChatException(ChatErrorCode.INVALID_IMAGE_TYPE);
        }
    }

    private String normalizeContent(String content) {
        return content.trim();
    }

    private boolean isHttpUrl(String content) {
        return content.startsWith("https://") || content.startsWith("http://");
    }

    private void publishAfterCommit(
            Conversation conversation,
            ChatEventResponse event
    ) {
        publishAfterCommit(conversation.getParticipantOne().getEmail(), event);
        publishAfterCommit(conversation.getParticipantTwo().getEmail(), event);
    }

    private void publishAfterCommit(String username, ChatEventResponse event) {
        runAfterCommit(() -> messagingTemplate.convertAndSendToUser(
                username,
                PRIVATE_DESTINATION,
                event
        ));
    }

    private void registerImageRollback(String publicId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status != STATUS_COMMITTED) {
                            imageStorage.delete(publicId);
                        }
                    }
                }
        );
    }

    private void runAfterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        action.run();
                    }
                }
        );
    }
}
