package com.techstore.user.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.techstore.user.config.CloudinaryProperties;
import com.techstore.user.exception.UserErrorCode;
import com.techstore.user.exception.UserModuleException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class CloudinaryAvatarStorage implements AvatarStorage {

    private final Cloudinary cloudinary;
    private final CloudinaryProperties properties;

    public CloudinaryAvatarStorage(
            Cloudinary cloudinary,
            CloudinaryProperties properties
    ) {
        this.cloudinary = cloudinary;
        this.properties = properties;
    }

    @Override
    public StoredAvatar upload(Long userId, MultipartFile file) {
        String publicId = "user-" + userId + "-" + UUID.randomUUID();
        try {
            Map<?, ?> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", properties.avatarFolder(),
                            "public_id", publicId,
                            "resource_type", "image",
                            "allowed_formats", List.of("jpg", "jpeg", "png", "webp"),
                            "transformation", "c_fill,g_face,h_512,w_512/q_auto,f_auto"
                    )
            );
            String secureUrl = requiredValue(result, "secure_url");
            String uploadedPublicId = requiredValue(result, "public_id");
            return new StoredAvatar(secureUrl, uploadedPublicId);
        } catch (IOException | RuntimeException exception) {
            throw new UserModuleException(UserErrorCode.AVATAR_UPLOAD_FAILED, exception);
        }
    }

    @Override
    public void delete(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            return;
        }
        try {
            cloudinary.uploader().destroy(
                    publicId,
                    ObjectUtils.asMap("resource_type", "image", "invalidate", true)
            );
        } catch (IOException | RuntimeException ignored) {
            // The new avatar is already persisted; stale assets can be cleaned asynchronously.
        }
    }

    private String requiredValue(Map<?, ?> result, String key) {
        Object value = result.get(key);
        if (value == null || value.toString().isBlank()) {
            throw new UserModuleException(UserErrorCode.AVATAR_UPLOAD_FAILED);
        }
        return value.toString();
    }
}
