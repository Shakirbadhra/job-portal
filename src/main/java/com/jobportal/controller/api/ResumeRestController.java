package com.jobportal.controller.api;

import com.jobportal.dto.response.ApiResponse;
import com.jobportal.dto.response.ResumeResponseDto;
import com.jobportal.entity.JobSeeker;
import com.jobportal.entity.Resume;
import com.jobportal.exception.UnauthorizedException;
import com.jobportal.entity.User;
import com.jobportal.security.SecurityUtils;
import com.jobportal.service.ResumeService;
import com.jobportal.service.UserService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/resumes")
public class ResumeRestController {

    private final ResumeService resumeService;
    private final UserService userService;
    private final SecurityUtils securityUtils;

    public ResumeRestController(ResumeService resumeService,
                                UserService userService,
                                SecurityUtils securityUtils) {
        this.resumeService = resumeService;
        this.userService = userService;
        this.securityUtils = securityUtils;
    }

    @PostMapping("/upload")
    @PreAuthorize("hasAuthority('ROLE_JOB_SEEKER')")
    public ResponseEntity<ApiResponse<ResumeResponseDto>> uploadResume(@RequestParam("file") MultipartFile file) {
        User user = securityUtils.getCurrentUserOrThrow();
        JobSeeker seeker = userService.getJobSeeker(user);
        ResumeResponseDto uploaded = resumeService.uploadResume(file, seeker);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Resume uploaded successfully!", uploaded));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_JOB_SEEKER')")
    public ResponseEntity<ApiResponse<List<ResumeResponseDto>>> getMyResumes() {
        User user = securityUtils.getCurrentUserOrThrow();
        JobSeeker seeker = userService.getJobSeeker(user);
        List<ResumeResponseDto> resumes = resumeService.getSeekerResumes(seeker);
        return ResponseEntity.ok(ApiResponse.success("Resumes fetched", resumes));
    }

    @PutMapping("/{id}/primary")
    @PreAuthorize("hasAuthority('ROLE_JOB_SEEKER')")
    public ResponseEntity<ApiResponse<Void>> setPrimaryResume(@PathVariable Long id) {
        User user = securityUtils.getCurrentUserOrThrow();
        JobSeeker seeker = userService.getJobSeeker(user);
        resumeService.setPrimaryResume(id, seeker);
        return ResponseEntity.ok(ApiResponse.success("Primary resume updated"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_JOB_SEEKER')")
    public ResponseEntity<ApiResponse<Void>> deleteResume(@PathVariable Long id) {
        User user = securityUtils.getCurrentUserOrThrow();
        JobSeeker seeker = userService.getJobSeeker(user);
        resumeService.deleteResume(id, seeker);
        return ResponseEntity.ok(ApiResponse.success("Resume deleted successfully"));
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadResume(@PathVariable Long id,
                                                   @RequestParam(defaultValue = "false") boolean inline) {
        User user = securityUtils.getCurrentUserOrThrow();
        if (!resumeService.canAccessResume(id, user)) {
            throw new UnauthorizedException("You do not have access to this resume");
        }
        Resume resume = resumeService.getResumeEntity(id);
        Resource resource = resumeService.loadResumeResource(id);

        String contentType = resume.getContentType();
        if (contentType == null || contentType.isEmpty()) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, (inline ? "inline" : "attachment")
                        + "; filename=\"" + resume.getOriginalFilename().replace("\"", "") + "\"")
                .body(resource);
    }
}
