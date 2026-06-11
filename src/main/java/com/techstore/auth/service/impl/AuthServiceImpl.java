package com.techstore.auth.service.impl;

import com.techstore.auth.dto.request.ChangePasswordRequest;
import com.techstore.auth.dto.request.LoginRequest;
import com.techstore.auth.dto.request.RefreshTokenRequest;
import com.techstore.auth.dto.request.RegisterRequest;
import com.techstore.auth.dto.request.UpdateProfileRequest;
import com.techstore.auth.dto.response.AuthResponse;
import com.techstore.auth.dto.response.ProfileResponse;
import com.techstore.auth.entity.RefreshToken;
import com.techstore.auth.entity.Role;
import com.techstore.auth.entity.User;
import com.techstore.auth.exception.AppException;
import com.techstore.auth.exception.ErrorCode;
import com.techstore.auth.repository.RefreshTokenRepository;
import com.techstore.auth.repository.UserRepository;
import com.techstore.auth.security.AuthUserPrincipal;
import com.techstore.auth.security.JwtService;
import com.techstore.auth.service.AuthService;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final Clock clock;

    public AuthServiceImpl(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.clock = clock;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User user = new User(
                email,
                passwordEncoder.encode(request.password()),
                request.fullName().trim(),
                normalizePhone(request.phone()),
                Role.ROLE_CUSTOMER
        );
        userRepository.saveAndFlush(user);
        return issueTokens(user);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            normalizeEmail(request.email()),
                            request.password()
                    )
            );
            AuthUserPrincipal principal = (AuthUserPrincipal) authentication.getPrincipal();
            User user = getUser(principal.getId());
            return issueTokens(user);
        } catch (DisabledException exception) {
            throw new AppException(ErrorCode.USER_DISABLED);
        } catch (AuthenticationException exception) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }
    }

    @Override
    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        JwtService.ParsedToken parsed = jwtService.parseRefreshToken(request.refreshToken());
        RefreshToken storedToken = refreshTokenRepository
                .findActiveByTokenIdForUpdate(parsed.tokenId())
                .orElseThrow(() -> new AppException(ErrorCode.REFRESH_TOKEN_REUSED));

        Instant now = clock.instant();
        User user = storedToken.getUser();
        boolean tokenMatchesUser = storedToken.isActive(now)
                && user.getId().equals(parsed.userId())
                && user.getTokenVersion() == parsed.tokenVersion()
                && user.isEnabled();
        if (!tokenMatchesUser) {
            storedToken.revoke(now);
            throw new AppException(ErrorCode.INVALID_TOKEN);
        }

        storedToken.revoke(now);
        return issueTokens(user);
    }

    @Override
    @Transactional
    public void logout(Long userId) {
        User user = getUser(userId);
        user.invalidateTokens();
        refreshTokenRepository.revokeAllActiveByUserId(userId, clock.instant());
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getProfile(Long userId) {
        return toProfile(getUser(userId));
    }

    @Override
    @Transactional
    public ProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = getUser(userId);
        user.updateProfile(
                request.fullName().trim(),
                normalizePhone(request.phone())
        );
        return toProfile(user);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = getUser(userId);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.CURRENT_PASSWORD_INCORRECT);
        }
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new AppException(ErrorCode.PASSWORD_CONFIRMATION_MISMATCH);
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.PASSWORD_UNCHANGED);
        }

        user.changePassword(passwordEncoder.encode(request.newPassword()));
        refreshTokenRepository.revokeAllActiveByUserId(userId, clock.instant());
    }

    private AuthResponse issueTokens(User user) {
        JwtService.GeneratedToken accessToken = jwtService.generateAccessToken(user);
        JwtService.GeneratedToken refreshToken = jwtService.generateRefreshToken(user);
        refreshTokenRepository.save(new RefreshToken(
                refreshToken.tokenId(),
                user,
                refreshToken.expiresAt(),
                clock.instant()
        ));

        return new AuthResponse(
                "Bearer",
                accessToken.value(),
                refreshToken.value(),
                jwtService.getAccessTokenExpiresInSeconds(),
                toProfile(user)
        );
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private ProfileResponse toProfile(User user) {
        return new ProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getRole(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        return phone.trim();
    }
}
