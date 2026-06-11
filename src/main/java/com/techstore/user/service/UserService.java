package com.techstore.user.service;

import com.techstore.user.dto.request.UpdateUserRequest;
import com.techstore.user.dto.response.AvatarResponse;
import com.techstore.user.dto.response.UserResponse;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {

    UserResponse getCurrentUser(Long userId);

    UserResponse getUser(Long userId);

    UserResponse updateUser(Long userId, UpdateUserRequest request);

    AvatarResponse uploadAvatar(Long userId, MultipartFile avatar);
}
