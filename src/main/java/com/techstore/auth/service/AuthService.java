package com.techstore.auth.service;

import com.techstore.auth.dto.request.ChangePasswordRequest;
import com.techstore.auth.dto.request.LoginRequest;
import com.techstore.auth.dto.request.RefreshTokenRequest;
import com.techstore.auth.dto.request.RegisterRequest;
import com.techstore.auth.dto.request.UpdateProfileRequest;
import com.techstore.auth.dto.response.AuthResponse;
import com.techstore.auth.dto.response.ProfileResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(RefreshTokenRequest request);

    void logout(Long userId);

    ProfileResponse getProfile(Long userId);

    ProfileResponse updateProfile(Long userId, UpdateProfileRequest request);

    void changePassword(Long userId, ChangePasswordRequest request);
}
