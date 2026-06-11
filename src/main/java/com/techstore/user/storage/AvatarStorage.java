package com.techstore.user.storage;

import org.springframework.web.multipart.MultipartFile;

public interface AvatarStorage {

    StoredAvatar upload(Long userId, MultipartFile file);

    void delete(String publicId);

    record StoredAvatar(String url, String publicId) {
    }
}
