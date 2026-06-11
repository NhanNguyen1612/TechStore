package com.techstore.auth.security;

import com.techstore.auth.entity.User;
import com.techstore.auth.exception.AppException;
import com.techstore.auth.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    public enum TokenType {
        ACCESS,
        REFRESH
    }

    public record GeneratedToken(String value, String tokenId, Instant expiresAt) {
    }

    public record ParsedToken(
            String subject,
            Long userId,
            String role,
            long tokenVersion,
            String tokenId,
            Instant expiresAt,
            TokenType type
    ) {
    }

    private final JwtProperties properties;
    private final Clock clock;
    private final SecretKey signingKey;

    public JwtService(JwtProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        try {
            this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.secret()));
        } catch (RuntimeException exception) {
            throw new IllegalStateException(
                    "JWT_SECRET must be a Base64-encoded key of at least 256 bits",
                    exception
            );
        }
    }

    public GeneratedToken generateAccessToken(User user) {
        return generateToken(user, TokenType.ACCESS, properties.accessTokenExpiration());
    }

    public GeneratedToken generateRefreshToken(User user) {
        return generateToken(user, TokenType.REFRESH, properties.refreshTokenExpiration());
    }

    public ParsedToken parseAccessToken(String token) {
        return parseToken(token, TokenType.ACCESS);
    }

    public ParsedToken parseRefreshToken(String token) {
        return parseToken(token, TokenType.REFRESH);
    }

    public long getAccessTokenExpiresInSeconds() {
        return properties.accessTokenExpiration().toSeconds();
    }

    private GeneratedToken generateToken(User user, TokenType type, Duration expiration) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(expiration);
        String tokenId = UUID.randomUUID().toString();

        String value = Jwts.builder()
                .issuer(properties.issuer())
                .subject(user.getEmail())
                .id(tokenId)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .claim("uid", user.getId())
                .claim("role", user.getRole().name())
                .claim("ver", user.getTokenVersion())
                .claim("type", type.name())
                .signWith(signingKey)
                .compact();

        return new GeneratedToken(value, tokenId, expiresAt);
    }

    private ParsedToken parseToken(String token, TokenType expectedType) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(properties.issuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            TokenType actualType = TokenType.valueOf(
                    claims.get("type", String.class)
            );
            if (actualType != expectedType) {
                throw new AppException(ErrorCode.INVALID_TOKEN);
            }

            Number userId = claims.get("uid", Number.class);
            Number tokenVersion = claims.get("ver", Number.class);
            if (userId == null || tokenVersion == null || claims.getId() == null) {
                throw new AppException(ErrorCode.INVALID_TOKEN);
            }

            return new ParsedToken(
                    claims.getSubject(),
                    userId.longValue(),
                    claims.get("role", String.class),
                    tokenVersion.longValue(),
                    claims.getId(),
                    claims.getExpiration().toInstant(),
                    actualType
            );
        } catch (ExpiredJwtException exception) {
            throw new AppException(ErrorCode.TOKEN_EXPIRED);
        } catch (AppException exception) {
            throw exception;
        } catch (JwtException | IllegalArgumentException exception) {
            throw new AppException(ErrorCode.INVALID_TOKEN);
        }
    }
}
