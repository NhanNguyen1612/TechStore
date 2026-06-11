package com.techstore.chat.repository;

import com.techstore.chat.entity.Conversation;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationRepository
        extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByParticipantKey(String participantKey);

    @EntityGraph(attributePaths = {"participantOne", "participantTwo", "assignedStaff"})
    @Query("""
            select conversation
            from Conversation conversation
            where conversation.id = :id
            """)
    Optional<Conversation> findDetailById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"participantOne", "participantTwo"})
    @Query(
            value = """
                    select conversation
                    from Conversation conversation
                    where conversation.participantOne.id = :userId
                       or conversation.participantTwo.id = :userId
                    """,
            countQuery = """
                    select count(conversation)
                    from Conversation conversation
                    where conversation.participantOne.id = :userId
                       or conversation.participantTwo.id = :userId
                    """
    )
    Page<Conversation> findAllForUser(
            @Param("userId") Long userId,
            Pageable pageable
    );
}
