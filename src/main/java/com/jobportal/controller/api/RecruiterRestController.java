package com.jobportal.controller.api;

import com.jobportal.dto.request.ApplicationStatusUpdateRequest;
import com.jobportal.dto.request.CompanyUpdateRequest;
import com.jobportal.dto.request.JobStatusUpdateRequest;
import com.jobportal.entity.enums.JobStatus;
import com.jobportal.dto.response.ApiResponse;
import com.jobportal.dto.response.ApplicationResponseDto;
import com.jobportal.dto.response.CompanyDto;
import com.jobportal.dto.response.DashboardStatsDto;
import com.jobportal.dto.response.JobSummaryDto;
import com.jobportal.entity.Recruiter;
import com.jobportal.entity.User;
import com.jobportal.security.SecurityUtils;
import com.jobportal.service.AnalyticsService;
import com.jobportal.service.ApplicationService;
import com.jobportal.service.CompanyService;
import com.jobportal.service.JobService;
import com.jobportal.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/recruiter")
@PreAuthorize("hasAuthority('ROLE_RECRUITER')")
public class RecruiterRestController {

    private final JobService jobService;
    private final ApplicationService applicationService;
    private final CompanyService companyService;
    private final UserService userService;
    private final AnalyticsService analyticsService;
    private final SecurityUtils securityUtils;

    public RecruiterRestController(JobService jobService,
                                   ApplicationService applicationService,
                                   CompanyService companyService,
                                   UserService userService,
                                   AnalyticsService analyticsService,
                                   SecurityUtils securityUtils) {
        this.jobService = jobService;
        this.applicationService = applicationService;
        this.companyService = companyService;
        this.userService = userService;
        this.analyticsService = analyticsService;
        this.securityUtils = securityUtils;
    }

    @GetMapping("/dashboard-stats")
    public ResponseEntity<ApiResponse<DashboardStatsDto>> getDashboardStats() {
        User user = securityUtils.getCurrentUserOrThrow();
        Recruiter recruiter = userService.getRecruiter(user);
        DashboardStatsDto stats = analyticsService.getRecruiterStats(recruiter);
        return ResponseEntity.ok(ApiResponse.success("Recruiter stats", stats));
    }

    @GetMapping("/jobs")
    public ResponseEntity<ApiResponse<Page<JobSummaryDto>>> getMyJobs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        User user = securityUtils.getCurrentUserOrThrow();
        Recruiter recruiter = userService.getRecruiter(user);
        Pageable pageable = PageRequest.of(page, size);
        Page<JobSummaryDto> result = jobService.getRecruiterJobs(recruiter, pageable);
        return ResponseEntity.ok(ApiResponse.success("Jobs fetched", result));
    }

    @PutMapping("/jobs/{jobId}/status")
    public ResponseEntity<ApiResponse<JobSummaryDto>> updateJobStatus(@PathVariable Long jobId,
                                                                      @Valid @RequestBody JobStatusUpdateRequest request) {
        User user = securityUtils.getCurrentUserOrThrow();
        Recruiter recruiter = userService.getRecruiter(user);
        JobSummaryDto updated = jobService.updateJobStatus(jobId, request.getStatus(), recruiter);
        String msg = request.getStatus() == JobStatus.CLOSED ? "Job closed to new applications" : "Job reopened";
        return ResponseEntity.ok(ApiResponse.success(msg, updated));
    }

    @GetMapping("/jobs/{jobId}/applications")
    public ResponseEntity<ApiResponse<Page<ApplicationResponseDto>>> getJobApplicants(
            @PathVariable Long jobId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        User user = securityUtils.getCurrentUserOrThrow();
        Recruiter recruiter = userService.getRecruiter(user);
        Pageable pageable = PageRequest.of(page, size);
        Page<ApplicationResponseDto> result = applicationService.getJobApplications(jobId, recruiter, pageable);
        return ResponseEntity.ok(ApiResponse.success("Job applicants", result));
    }

    @GetMapping("/applications")
    public ResponseEntity<ApiResponse<Page<ApplicationResponseDto>>> getAllApplicants(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        User user = securityUtils.getCurrentUserOrThrow();
        Recruiter recruiter = userService.getRecruiter(user);
        Pageable pageable = PageRequest.of(page, size);
        Page<ApplicationResponseDto> result = applicationService.getRecruiterApplications(recruiter.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success("All applicants", result));
    }

    @PutMapping("/applications/{id}/status")
    public ResponseEntity<ApiResponse<ApplicationResponseDto>> updateApplicationStatus(
            @PathVariable Long id,
            @Valid @RequestBody ApplicationStatusUpdateRequest request) {
        User user = securityUtils.getCurrentUserOrThrow();
        Recruiter recruiter = userService.getRecruiter(user);
        ApplicationResponseDto updated = applicationService.updateApplicationStatus(id, request, recruiter);
        return ResponseEntity.ok(ApiResponse.success("Application status updated", updated));
    }

    @PutMapping("/company")
    public ResponseEntity<ApiResponse<CompanyDto>> updateCompany(@Valid @RequestBody CompanyUpdateRequest request) {
        User user = securityUtils.getCurrentUserOrThrow();
        Recruiter recruiter = userService.getRecruiter(user);
        if (recruiter.getCompany() == null) {
            recruiter.setCompany(companyService.getOrCreateCompany(request.getName(), request.getWebsite(), request.getIndustry()));
        }
        CompanyDto dto = companyService.updateCompany(recruiter.getCompany().getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Company profile updated", dto));
    }
}
