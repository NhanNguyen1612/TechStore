package com.techstore.product.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.techstore.product.config.ProductImageProperties;
import com.techstore.product.exception.ProductErrorCode;
import com.techstore.product.exception.ProductException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class CloudinaryProductImageStorage implements ProductImageStorage {

    private final Cloudinary cloudinary;
    private final ProductImageProperties properties;

    public CloudinaryProductImageStorage(
            Cloudinary cloudinary,
            ProductImageProperties properties
    ) {
        this.cloudinary = cloudinary;
        this.properties = properties;
    }

    @Override
    public List<StoredImage> upload(List<MultipartFile> files) {
        List<StoredImage> uploaded = new ArrayList<>();
        try {
            for (MultipartFile file : files) {
                Map<?, ?> result = cloudinary.uploader().upload(
                        file.getBytes(),
                        ObjectUtils.asMap(
                                "folder", properties.folder(),
                                "public_id", "product-" + UUID.randomUUID(),
                                "resource_type", "image",
                                "allowed_formats", List.of("jpg", "jpeg", "png", "webp"),
                                "transformation", "c_limit,h_1600,w_1600/q_auto,f_auto"
                        )
                );
                uploaded.add(new StoredImage(
                        requiredValue(result, "secure_url"),
                        requiredValue(result, "public_id")
                ));
            }
            return uploaded;
        } catch (IOException | RuntimeException exception) {
            uploaded.forEach(image -> delete(image.publicId()));
            if (exception instanceof ProductException productException) {
                throw productException;
            }
            throw new ProductException(ProductErrorCode.IMAGE_UPLOAD_FAILED, exception);
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
            // A later cleanup job can remove stale Cloudinary resources.
        }
    }

    private String requiredValue(Map<?, ?> result, String key) {
        Object value = result.get(key);
        if (value == null || value.toString().isBlank()) {
            throw new ProductException(ProductErrorCode.IMAGE_UPLOAD_FAILED);
        }
        return value.toString();
    }
}
