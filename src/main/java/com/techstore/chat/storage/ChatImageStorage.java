package com.techstore.chat.storage;

import org.springframework.web.multipart.MultipartFile;

public interface ChatImageStorage {

    StoredImage upload(MultipartFile file);

    void delete(String publicId);

    record StoredImage(String url, String publicId) {
    }
}
