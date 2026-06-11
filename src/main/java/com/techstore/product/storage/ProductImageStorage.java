package com.techstore.product.storage;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface ProductImageStorage {

    List<StoredImage> upload(List<MultipartFile> files);

    void delete(String publicId);

    record StoredImage(String url, String publicId) {
    }
}
