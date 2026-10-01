package com.jobportal.service;

import com.jobportal.dto.request.ProfileUpdateRequest;
import com.jobportal.dto.request.RegisterRequest;
import com.jobportal.dto.response.UserProfileDto;
import com.jobportal.entity.JobSeeker;
import com.jobportal.entity.Recruiter;
import com.jobportal.entity.User;

import java.util.List;

public interface UserService {
    User register(RegisterRequest request);
    UserProfileDto getUserProfile(User user);
    UserProfileDto updateProfile(User user, ProfileUpdateRequest request);
    List<UserProfileDto> getAllUsers();
    boolean toggleUserStatus(Long userId);
    JobSeeker getJobSeeker(User user);
    Recruiter getRecruiter(User user);

    void changePassword(User user, String currentPassword, String newPassword);
}
