package com.jobportal.controller.api;

import com.jobportal.dto.response.ApiResponse;
import com.jobportal.dto.response.ApplicationResponseDto;
import com.jobportal.dto.response.DashboardStatsDto;
import com.jobportal.dto.response.JobDetailDto;
import com.jobportal.dto.response.UserProfileDto;
import com.jobportal.entity.enums.JobStatus;
import com.jobportal.service.AnalyticsService;
import com.jobportal.service.ApplicationService;
import com.jobportal.service.JobService;
import com.jobportal.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.jobportal.entity.ContactMessage;
import com.jobportal.exception.BadRequestException;
import com.jobportal.exception.ResourceNotFoundException;
import com.jobportal.repository.ContactMessageRepository;
import com.jobportal.security.SecurityUtils;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminRestController {

    private final UserService userService;
    private final JobService jobService;
    private final ApplicationService applicationService;
    private final AnalyticsService analyticsService;
    private final ContactMessageRepository contactMessageRepository;
    private final SecurityUtils securityUtils;

    public AdminRestController(UserService userService,
                               JobService jobService,
                               ApplicationService applicationService,
                               AnalyticsService analyticsService,
                               ContactMessageRepository contactMessageRepository,
                               SecurityUtils securityUtils) {
        this.contactMessageRepository = contactMessageRepository;
        this.securityUtils = securityUtils;
        this.userService = userService;
        this.jobService = jobService;
        this.applicationService = applicationService;
        this.analyticsService = analyticsService;
    }

    @GetMapping("/dashboard-stats")
    public ResponseEntity<ApiResponse<DashboardStatsDto>> getDashboardStats() {
        return ResponseEntity.ok(ApiResponse.success("Admin stats", analyticsService.getAdminStats()));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserProfileDto>>> getAllUsers() {
        return ResponseEntity.ok(ApiResponse.success("All users", userService.getAllUsers()));
    }

    @PutMapping("/users/{id}/toggle-status")
    public ResponseEntity<ApiResponse<Boolean>> toggleUserStatus(@PathVariable Long id) {
        if (securityUtils.getCurrentUserOrThrow().getId().equals(id)) {
            throw new BadRequestException("You cannot disable your own administrator account");
        }
        boolean active = userService.toggleUserStatus(id);
        String msg = active ? "User enabled successfully" : "User disabled successfully";
        return ResponseEntity.ok(ApiResponse.success(msg, active));
    }

    @GetMapping("/jobs")
    public ResponseEntity<ApiResponse<Page<JobDetailDto>>> getAllJobs(
            @RequestParam(required = false) JobStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<JobDetailDto> jobs = jobService.getAllJobsForAdmin(status, pageable);
        return ResponseEntity.ok(ApiResponse.success("Jobs fetched", jobs));
    }

    @PutMapping("/jobs/{id}/approve")
    public ResponseEntity<ApiResponse<Boolean>> approveJob(@PathVariable Long id) {
        jobService.approveJob(id);
        return ResponseEntity.ok(ApiResponse.success("Job approved successfully", true));
    }

    @PutMapping("/jobs/{id}/reject")
    public ResponseEntity<ApiResponse<Boolean>> rejectJob(@PathVariable Long id) {
        jobService.rejectJob(id);
        return ResponseEntity.ok(ApiResponse.success("Job rejected", true));
    }

    @DeleteMapping("/jobs/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteJob(@PathVariable Long id) {
        jobService.deleteJobByAdmin(id);
        return ResponseEntity.ok(ApiResponse.success("Job permanently removed"));
    }

    @GetMapping("/applications")
    public ResponseEntity<ApiResponse<Page<ApplicationResponseDto>>> getAllApplications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "appliedAt"));
        Page<ApplicationResponseDto> applications = applicationService.getAllApplicationsForAdmin(pageable);
        return ResponseEntity.ok(ApiResponse.success("All applications", applications));
    }

    @PutMapping("/jobs/{id}/feature")
    public ResponseEntity<ApiResponse<Boolean>> toggleFeatured(@PathVariable Long id) {
        boolean featured = jobService.toggleFeatured(id);
        return ResponseEntity.ok(ApiResponse.success(featured ? "Job marked as featured" : "Job removed from featured", featured));
    }

    @GetMapping("/messages")
    public ResponseEntity<ApiResponse<List<ContactMessage>>> getMessages() {
        return ResponseEntity.ok(ApiResponse.success("Messages fetched", contactMessageRepository.findAllByOrderByCreatedAtDesc()));
    }

    @PutMapping("/messages/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markMessageRead(@PathVariable Long id) {
        ContactMessage message = contactMessageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found with id: " + id));
        message.setRead(true);
        contactMessageRepository.save(message);
        return ResponseEntity.ok(ApiResponse.success("Message marked as read"));
    }

    @DeleteMapping("/messages/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMessage(@PathVariable Long id) {
        if (!contactMessageRepository.existsById(id)) {
            throw new ResourceNotFoundException("Message not found with id: " + id);
        }
        contactMessageRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("Message deleted"));
    }
}
