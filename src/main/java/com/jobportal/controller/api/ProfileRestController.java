package com.jobportal.controller.api;

import com.jobportal.dto.request.ProfileUpdateRequest;
import com.jobportal.dto.response.ApiResponse;
import com.jobportal.dto.response.UserProfileDto;
import com.jobportal.entity.User;
import com.jobportal.security.SecurityUtils;
import com.jobportal.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/profile")
public class ProfileRestController {

    private final UserService userService;
    private final SecurityUtils securityUtils;

    public ProfileRestController(UserService userService, SecurityUtils securityUtils) {
        this.userService = userService;
        this.securityUtils = securityUtils;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<UserProfileDto>> getProfile() {
        User user = securityUtils.getCurrentUserOrThrow();
        UserProfileDto profile = userService.getUserProfile(user);
        return ResponseEntity.ok(ApiResponse.success("Profile fetched", profile));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<UserProfileDto>> updateProfile(@Valid @RequestBody ProfileUpdateRequest request) {
        User user = securityUtils.getCurrentUserOrThrow();
        UserProfileDto updated = userService.updateProfile(user, request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", updated));
    }
}
