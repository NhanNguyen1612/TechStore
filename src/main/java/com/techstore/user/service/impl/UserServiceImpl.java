package com.techstore.user.service.impl;

import com.techstore.auth.entity.Role;
import com.techstore.auth.entity.User;
import com.techstore.user.config.CloudinaryProperties;
import com.techstore.user.dto.request.UpdateUserRequest;
import com.techstore.user.dto.response.AvatarResponse;
import com.techstore.user.dto.response.UserResponse;
import com.techstore.user.entity.UserProfile;
import com.techstore.user.exception.UserErrorCode;
import com.techstore.user.exception.UserModuleException;
import com.techstore.user.repository.UserManagementRepository;
import com.techstore.user.repository.UserProfileRepository;
import com.techstore.user.service.UserService;
import com.techstore.user.storage.AvatarStorage;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UserServiceImpl implements UserService {

    private static final Set<String> ALLOWED_AVATAR_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final UserManagementRepository userRepository;
    private final UserProfileRepository profileRepository;
    private final AvatarStorage avatarStorage;
    private final CloudinaryProperties cloudinaryProperties;

    public UserServiceImpl(
            UserManagementRepository userRepository,
            UserProfileRepository profileRepository,
            AvatarStorage avatarStorage,
            CloudinaryProperties cloudinaryProperties
    ) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.avatarStorage = avatarStorage;
        this.cloudinaryProperties = cloudinaryProperties;
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(Long userId) {
        return getUser(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUser(Long userId) {
        User user = findUser(userId);
        return toResponse(user, profileRepository.findById(userId).orElse(null));
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long userId, UpdateUserRequest request) {
        User user = findUser(userId);
        boolean accessChanged = user.getRole() != request.role()
                || user.isEnabled() != request.enabled();

        protectLastAdministrator(user, request);
        user.updateProfile(request.fullName().trim(), normalizePhone(request.phone()));
        userRepository.saveAndFlush(user);

        if (accessChanged) {
            userRepository.updateAccessAndInvalidateTokens(
                    userId,
                    request.role(),
                    request.enabled()
            );
            user = findUser(userId);
        }

        return toResponse(user, profileRepository.findById(userId).orElse(null));
    }

    @Override
    @Transactional
    public AvatarResponse uploadAvatar(Long userId, MultipartFile avatar) {
        validateAvatar(avatar);
        User user = findUser(userId);
        UserProfile profile = profileRepository.findById(userId)
                .orElseGet(() -> new UserProfile(user));
        String oldPublicId = profile.getAvatarPublicId();

        AvatarStorage.StoredAvatar uploaded = avatarStorage.upload(userId, avatar);
        try {
            profile.updateAvatar(uploaded.url(), uploaded.publicId());
            profileRepository.saveAndFlush(profile);
        } catch (RuntimeException exception) {
            avatarStorage.delete(uploaded.publicId());
            throw exception;
        }

        registerAvatarCleanup(oldPublicId, uploaded.publicId());
        return new AvatarResponse(uploaded.url());
    }

    private void registerAvatarCleanup(String oldPublicId, String newPublicId) {
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status == STATUS_COMMITTED) {
                            avatarStorage.delete(oldPublicId);
                        } else {
                            avatarStorage.delete(newPublicId);
                        }
                    }
                }
        );
    }

    private void validateAvatar(MultipartFile avatar) {
        if (avatar == null || avatar.isEmpty()) {
            throw new UserModuleException(UserErrorCode.AVATAR_REQUIRED);
        }
        if (avatar.getSize() > cloudinaryProperties.maxAvatarSize().toBytes()) {
            throw new UserModuleException(UserErrorCode.AVATAR_TOO_LARGE);
        }

        String contentType = Optional.ofNullable(avatar.getContentType())
                .orElse("")
                .toLowerCase(Locale.ROOT);
        if (!ALLOWED_AVATAR_TYPES.contains(contentType)) {
            throw new UserModuleException(UserErrorCode.INVALID_AVATAR_TYPE);
        }
    }

    private void protectLastAdministrator(User user, UpdateUserRequest request) {
        boolean removesActiveAdmin = user.getRole() == Role.ROLE_ADMIN
                && user.isEnabled()
                && (request.role() != Role.ROLE_ADMIN || !request.enabled());
        if (removesActiveAdmin
                && userRepository.countByRoleAndEnabledTrue(Role.ROLE_ADMIN) <= 1) {
            throw new UserModuleException(UserErrorCode.LAST_ADMIN_PROTECTED);
        }
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserModuleException(UserErrorCode.USER_NOT_FOUND));
    }

    private UserResponse toResponse(User user, UserProfile profile) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                profile == null ? null : profile.getAvatarUrl(),
                user.getRole(),
                user.isEnabled(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                profile == null ? null : profile.getCreatedBy(),
                profile == null ? null : profile.getUpdatedBy()
        );
    }

    private String normalizePhone(String phone) {
        return phone == null || phone.isBlank() ? null : phone.trim();
    }
}
