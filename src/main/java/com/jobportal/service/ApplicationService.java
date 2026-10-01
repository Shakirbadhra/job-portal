package com.jobportal.service;

import com.jobportal.dto.request.ApplicationStatusUpdateRequest;
import com.jobportal.dto.request.ApplicationSubmitRequest;
import com.jobportal.dto.response.ApplicationResponseDto;
import com.jobportal.entity.JobSeeker;
import com.jobportal.entity.Recruiter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ApplicationService {
    ApplicationResponseDto apply(ApplicationSubmitRequest request, JobSeeker seeker);
    boolean hasApplied(Long jobId, JobSeeker seeker);
    Page<ApplicationResponseDto> getSeekerApplications(JobSeeker seeker, Pageable pageable);
    Page<ApplicationResponseDto> getRecruiterApplications(Long recruiterId, Pageable pageable);
    Page<ApplicationResponseDto> getJobApplications(Long jobId, Recruiter recruiter, Pageable pageable);
    ApplicationResponseDto getApplicationDetails(Long applicationId);

    /** Returns the application only if the viewer owns it (seeker), owns the job (recruiter) or is an admin. */
    ApplicationResponseDto getApplicationDetails(Long applicationId, com.jobportal.entity.User viewer);
    ApplicationResponseDto updateApplicationStatus(Long applicationId, ApplicationStatusUpdateRequest request, Recruiter recruiter);
    Page<ApplicationResponseDto> getAllApplicationsForAdmin(Pageable pageable);
}
