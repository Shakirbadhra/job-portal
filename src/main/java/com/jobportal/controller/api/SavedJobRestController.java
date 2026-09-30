package com.jobportal.controller.api;

import com.jobportal.dto.response.ApiResponse;
import com.jobportal.dto.response.JobSummaryDto;
import com.jobportal.entity.JobSeeker;
import com.jobportal.entity.User;
import com.jobportal.security.SecurityUtils;
import com.jobportal.service.SavedJobService;
import com.jobportal.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/saved-jobs")
@PreAuthorize("hasAuthority('ROLE_JOB_SEEKER')")
public class SavedJobRestController {

    private final SavedJobService savedJobService;
    private final UserService userService;
    private final SecurityUtils securityUtils;

    public SavedJobRestController(SavedJobService savedJobService,
                                  UserService userService,
                                  SecurityUtils securityUtils) {
        this.savedJobService = savedJobService;
        this.userService = userService;
        this.securityUtils = securityUtils;
    }

    @PostMapping("/{jobId}")
    public ResponseEntity<ApiResponse<Boolean>> toggleSaveJob(@PathVariable Long jobId) {
        User user = securityUtils.getCurrentUserOrThrow();
        JobSeeker seeker = userService.getJobSeeker(user);

        boolean isSaved = savedJobService.isJobSaved(jobId, seeker);
        if (isSaved) {
            savedJobService.removeSavedJob(jobId, seeker);
            return ResponseEntity.ok(ApiResponse.success("Job removed from saved list", false));
        } else {
            savedJobService.saveJob(jobId, seeker);
            return ResponseEntity.ok(ApiResponse.success("Job saved successfully", true));
        }
    }

    @DeleteMapping("/{jobId}")
    public ResponseEntity<ApiResponse<Void>> removeSavedJob(@PathVariable Long jobId) {
        User user = securityUtils.getCurrentUserOrThrow();
        JobSeeker seeker = userService.getJobSeeker(user);
        savedJobService.removeSavedJob(jobId, seeker);
        return ResponseEntity.ok(ApiResponse.success("Job removed from saved list"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<JobSummaryDto>>> getSavedJobs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        User user = securityUtils.getCurrentUserOrThrow();
        JobSeeker seeker = userService.getJobSeeker(user);
        Pageable pageable = PageRequest.of(page, size);
        Page<JobSummaryDto> result = savedJobService.getSavedJobs(seeker, pageable);
        return ResponseEntity.ok(ApiResponse.success("Saved jobs fetched", result));
    }
}
