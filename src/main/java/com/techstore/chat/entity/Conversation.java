package com.techstore.chat.entity;

import com.techstore.auth.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(
        name = "conversations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_conversations_participant_key",
                columnNames = "participant_key"
        )
)
@EntityListeners(AuditingEntityListener.class)
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_one_id", nullable = false)
    private User participantOne;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_two_id", nullable = false)
    private User participantTwo;

    @Column(name = "participant_key", nullable = false, length = 50)
    private String participantKey;

    @Column(name = "last_message_at")
    private Instant lastMessageAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_staff_id")
    private User assignedStaff;

    @Column(nullable = false)
    private boolean closed;

    @Column(name = "closed_at")
    private Instant closedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected Conversation() {
    }

    public Conversation(User first, User second) {
        if (first.getId().equals(second.getId())) {
            throw new IllegalArgumentException("Conversation participants must differ");
        }
        if (first.getId() < second.getId()) {
            this.participantOne = first;
            this.participantTwo = second;
        } else {
            this.participantOne = second;
            this.participantTwo = first;
        }
        this.participantKey = participantKey(first.getId(), second.getId());
    }

    public static String participantKey(Long firstId, Long secondId) {
        long lower = Math.min(firstId, secondId);
        long upper = Math.max(firstId, secondId);
        return lower + ":" + upper;
    }

    public boolean hasParticipant(Long userId) {
        return participantOne.getId().equals(userId)
                || participantTwo.getId().equals(userId);
    }

    public User otherParticipant(Long userId) {
        if (participantOne.getId().equals(userId)) {
            return participantTwo;
        }
        if (participantTwo.getId().equals(userId)) {
            return participantOne;
        }
        throw new IllegalArgumentException("User is not a conversation participant");
    }

    public void touch(Instant messageAt) {
        this.lastMessageAt = messageAt;
    }

    public void assignStaff(User staff) {
        this.assignedStaff = staff;
    }

    public void close(Instant closedAt) {
        this.closed = true;
        this.closedAt = closedAt;
    }

    public Long getId() {
        return id;
    }

    public User getParticipantOne() {
        return participantOne;
    }

    public User getParticipantTwo() {
        return participantTwo;
    }

    public String getParticipantKey() {
        return participantKey;
    }

    public Instant getLastMessageAt() {
        return lastMessageAt;
    }

    public User getAssignedStaff() {
        return assignedStaff;
    }

    public boolean isClosed() {
        return closed;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
