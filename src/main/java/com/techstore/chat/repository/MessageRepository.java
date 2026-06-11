package com.techstore.chat.repository;

import com.techstore.chat.entity.Message;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, Long> {

    @EntityGraph(attributePaths = {"sender"})
    Page<Message> findAllByConversationId(
            Long conversationId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"sender"})
    Optional<Message> findFirstByConversationIdOrderByCreatedAtDescIdDesc(
            Long conversationId
    );

    long countByConversationIdAndSenderIdNotAndReadAtIsNull(
            Long conversationId,
            Long userId
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Message message
            set message.readAt = :readAt
            where message.conversation.id = :conversationId
              and message.sender.id <> :readerId
              and message.readAt is null
            """)
    int markUnreadAsRead(
            @Param("conversationId") Long conversationId,
            @Param("readerId") Long readerId,
            @Param("readAt") Instant readAt
    );
}
