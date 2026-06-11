package com.techstore.auth.repository;

import com.techstore.auth.entity.RefreshToken;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select token
            from RefreshToken token
            join fetch token.user
            where token.tokenId = :tokenId
              and token.revokedAt is null
            """)
    Optional<RefreshToken> findActiveByTokenIdForUpdate(@Param("tokenId") String tokenId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update RefreshToken token
            set token.revokedAt = :revokedAt
            where token.user.id = :userId
              and token.revokedAt is null
            """)
    int revokeAllActiveByUserId(
            @Param("userId") Long userId,
            @Param("revokedAt") Instant revokedAt
    );
}
