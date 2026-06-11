package com.techstore.auth.controller;

import com.techstore.auth.dto.request.ChangePasswordRequest;
import com.techstore.auth.dto.request.LoginRequest;
import com.techstore.auth.dto.request.RefreshTokenRequest;
import com.techstore.auth.dto.request.RegisterRequest;
import com.techstore.auth.dto.request.UpdateProfileRequest;
import com.techstore.auth.dto.response.ApiResponse;
import com.techstore.auth.dto.response.AuthResponse;
import com.techstore.auth.dto.response.ProfileResponse;
import com.techstore.auth.security.AuthUserPrincipal;
import com.techstore.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration successful", authService.register(request)));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success("Login successful", authService.login(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        return ApiResponse.success("Token refreshed", authService.refresh(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@AuthenticationPrincipal AuthUserPrincipal principal) {
        authService.logout(principal.getId());
        return ApiResponse.success("Logout successful");
    }

    @GetMapping("/me")
    public ApiResponse<ProfileResponse> getProfile(
            @AuthenticationPrincipal AuthUserPrincipal principal
    ) {
        return ApiResponse.success("Profile retrieved", authService.getProfile(principal.getId()));
    }

    @PutMapping("/profile")
    public ApiResponse<ProfileResponse> updateProfile(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return ApiResponse.success(
                "Profile updated",
                authService.updateProfile(principal.getId(), request)
        );
    }

    @PutMapping("/change-password")
    public ApiResponse<Void> changePassword(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        authService.changePassword(principal.getId(), request);
        return ApiResponse.success("Password changed successfully");
    }
}
