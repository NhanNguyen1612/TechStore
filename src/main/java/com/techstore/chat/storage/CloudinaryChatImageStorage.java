package com.techstore.chat.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.techstore.chat.config.ChatImageProperties;
import com.techstore.chat.exception.ChatErrorCode;
import com.techstore.chat.exception.ChatException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class CloudinaryChatImageStorage implements ChatImageStorage {

    private final Cloudinary cloudinary;
    private final ChatImageProperties properties;

    public CloudinaryChatImageStorage(
            Cloudinary cloudinary,
            ChatImageProperties properties
    ) {
        this.cloudinary = cloudinary;
        this.properties = properties;
    }

    @Override
    public StoredImage upload(MultipartFile file) {
        try {
            Map<?, ?> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", properties.folder(),
                            "public_id", "message-" + UUID.randomUUID(),
                            "resource_type", "image",
                            "allowed_formats", List.of("jpg", "jpeg", "png", "webp"),
                            "transformation", "c_limit,h_1600,w_1600/q_auto,f_auto"
                    )
            );
            return new StoredImage(
                    requiredValue(result, "secure_url"),
                    requiredValue(result, "public_id")
            );
        } catch (IOException | RuntimeException exception) {
            if (exception instanceof ChatException chatException) {
                throw chatException;
            }
            throw new ChatException(ChatErrorCode.IMAGE_UPLOAD_FAILED, exception);
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
            // Failed rollback cleanup can be retried by an asset maintenance job.
        }
    }

    private String requiredValue(Map<?, ?> result, String key) {
        Object value = result.get(key);
        if (value == null || value.toString().isBlank()) {
            throw new ChatException(ChatErrorCode.IMAGE_UPLOAD_FAILED);
        }
        return value.toString();
    }
}
