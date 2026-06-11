package com.techstore.auth.dto.response;

public record AuthResponse(
        String tokenType,
        String accessToken,
        String refreshToken,
        long expiresIn,
        ProfileResponse user
) {
}
