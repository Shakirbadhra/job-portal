package com.jobportal.service;

import com.jobportal.dto.response.JobSummaryDto;
import com.jobportal.entity.JobSeeker;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SavedJobService {
    boolean saveJob(Long jobId, JobSeeker seeker);
    boolean removeSavedJob(Long jobId, JobSeeker seeker);
    boolean isJobSaved(Long jobId, JobSeeker seeker);
    Page<JobSummaryDto> getSavedJobs(JobSeeker seeker, Pageable pageable);
}
