package com.jobportal.service;

import com.jobportal.dto.request.JobCreateUpdateRequest;
import com.jobportal.dto.request.JobSearchCriteria;
import com.jobportal.dto.response.JobDetailDto;
import com.jobportal.dto.response.JobSummaryDto;
import com.jobportal.entity.Job;
import com.jobportal.entity.JobSeeker;
import com.jobportal.entity.Recruiter;
import com.jobportal.entity.User;
import com.jobportal.entity.enums.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface JobService {
    Page<JobSummaryDto> searchJobs(JobSearchCriteria criteria, Pageable pageable, User currentUser);
    JobDetailDto getJobDetails(Long id, User currentUser);
    List<JobSummaryDto> getFeaturedJobs(User currentUser);
    List<JobSummaryDto> getRecentJobs(User currentUser);
    List<String> getCategories();
    JobDetailDto createJob(JobCreateUpdateRequest request, Recruiter recruiter);
    JobDetailDto updateJob(Long jobId, JobCreateUpdateRequest request, Recruiter recruiter);
    void deleteJob(Long jobId, Recruiter recruiter);
    void deleteJobByAdmin(Long jobId);
    Page<JobSummaryDto> getRecruiterJobs(Recruiter recruiter, Pageable pageable);
    Page<JobDetailDto> getAllJobsForAdmin(JobStatus status, Pageable pageable);
    boolean approveJob(Long jobId);
    boolean rejectJob(Long jobId);
    List<JobSummaryDto> getRecommendedJobs(JobSeeker seeker, int limit);
    Job getJobEntity(Long jobId);

    JobDetailDto getJobForRecruiter(Long jobId, Recruiter recruiter);

    JobSummaryDto updateJobStatus(Long jobId, JobStatus status, Recruiter recruiter);

    boolean toggleFeatured(Long jobId);

    java.util.Map<String, Long> getCategoryCounts();
}
