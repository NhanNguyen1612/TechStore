package com.techstore.review.storage;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface ReviewImageStorage {

    List<StoredImage> upload(List<MultipartFile> files);

    void delete(String publicId);

    record StoredImage(String url, String publicId) {
    }
}
