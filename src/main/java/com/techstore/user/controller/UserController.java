package com.techstore.user.controller;

import com.techstore.auth.dto.response.ApiResponse;
import com.techstore.auth.security.AuthUserPrincipal;
import com.techstore.user.dto.request.UpdateUserRequest;
import com.techstore.user.dto.response.AvatarResponse;
import com.techstore.user.dto.response.UserResponse;
import com.techstore.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> getCurrentUser(
            @AuthenticationPrincipal AuthUserPrincipal principal
    ) {
        return ApiResponse.success(
                "User profile retrieved",
                userService.getCurrentUser(principal.getId())
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> getUser(@PathVariable Long id) {
        return ApiResponse.success("User retrieved", userService.getUser(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        return ApiResponse.success("User updated", userService.updateUser(id, request));
    }

    @PutMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<AvatarResponse> uploadAvatar(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @RequestParam(value = "avatar", required = false) MultipartFile avatar
    ) {
        return ApiResponse.success(
                "Avatar uploaded",
                userService.uploadAvatar(principal.getId(), avatar)
        );
    }
}
