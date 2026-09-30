package com.jobportal.controller.api;

import com.jobportal.dto.request.ApplicationSubmitRequest;
import com.jobportal.dto.response.ApiResponse;
import com.jobportal.dto.response.ApplicationResponseDto;
import com.jobportal.entity.JobSeeker;
import com.jobportal.entity.User;
import com.jobportal.security.SecurityUtils;
import com.jobportal.service.ApplicationService;
import com.jobportal.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/applications")
public class ApplicationRestController {

    private final ApplicationService applicationService;
    private final UserService userService;
    private final SecurityUtils securityUtils;

    public ApplicationRestController(ApplicationService applicationService,
                                     UserService userService,
                                     SecurityUtils securityUtils) {
        this.applicationService = applicationService;
        this.userService = userService;
        this.securityUtils = securityUtils;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_JOB_SEEKER')")
    public ResponseEntity<ApiResponse<ApplicationResponseDto>> apply(@Valid @RequestBody ApplicationSubmitRequest request) {
        User user = securityUtils.getCurrentUserOrThrow();
        JobSeeker seeker = userService.getJobSeeker(user);
        ApplicationResponseDto response = applicationService.apply(request, seeker);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Application submitted successfully!", response));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAuthority('ROLE_JOB_SEEKER')")
    public ResponseEntity<ApiResponse<Page<ApplicationResponseDto>>> getMyApplications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        User user = securityUtils.getCurrentUserOrThrow();
        JobSeeker seeker = userService.getJobSeeker(user);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "appliedAt"));
        Page<ApplicationResponseDto> result = applicationService.getSeekerApplications(seeker, pageable);
        return ResponseEntity.ok(ApiResponse.success("Applications fetched", result));
    }

    @GetMapping("/check/{jobId}")
    @PreAuthorize("hasAuthority('ROLE_JOB_SEEKER')")
    public ResponseEntity<ApiResponse<Boolean>> checkApplied(@PathVariable Long jobId) {
        User user = securityUtils.getCurrentUserOrThrow();
        JobSeeker seeker = userService.getJobSeeker(user);
        boolean applied = applicationService.hasApplied(jobId, seeker);
        return ResponseEntity.ok(ApiResponse.success("Status checked", applied));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ApplicationResponseDto>> getApplicationDetails(@PathVariable Long id) {
        ApplicationResponseDto dto = applicationService.getApplicationDetails(id);
        return ResponseEntity.ok(ApiResponse.success("Application details", dto));
    }
}
