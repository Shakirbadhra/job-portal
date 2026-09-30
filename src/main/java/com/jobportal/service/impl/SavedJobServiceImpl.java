package com.jobportal.service.impl;

import com.jobportal.dto.response.JobSummaryDto;
import com.jobportal.entity.Job;
import com.jobportal.entity.JobSeeker;
import com.jobportal.entity.SavedJob;
import com.jobportal.exception.ResourceNotFoundException;
import com.jobportal.repository.JobRepository;
import com.jobportal.repository.SavedJobRepository;
import com.jobportal.service.SavedJobService;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SavedJobServiceImpl implements SavedJobService {

    private final SavedJobRepository savedJobRepository;
    private final JobRepository jobRepository;
    private final JobServiceImpl jobService;

    public SavedJobServiceImpl(SavedJobRepository savedJobRepository,
                               JobRepository jobRepository,
                               @Lazy JobServiceImpl jobService) {
        this.savedJobRepository = savedJobRepository;
        this.jobRepository = jobRepository;
        this.jobService = jobService;
    }

    @Override
    public boolean saveJob(Long jobId, JobSeeker seeker) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + jobId));

        if (!savedJobRepository.existsByJobSeekerAndJob(seeker, job)) {
            SavedJob savedJob = new SavedJob(seeker, job);
            savedJobRepository.save(savedJob);
            return true;
        }
        return false;
    }

    @Override
    public boolean removeSavedJob(Long jobId, JobSeeker seeker) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + jobId));

        return savedJobRepository.findByJobSeekerAndJob(seeker, job)
                .map(sj -> {
                    savedJobRepository.delete(sj);
                    return true;
                }).orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isJobSaved(Long jobId, JobSeeker seeker) {
        Job job = jobRepository.findById(jobId).orElse(null);
        if (job == null) return false;
        return savedJobRepository.existsByJobSeekerAndJob(seeker, job);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JobSummaryDto> getSavedJobs(JobSeeker seeker, Pageable pageable) {
        Page<SavedJob> page = savedJobRepository.findByJobSeekerOrderBySavedAtDesc(seeker, pageable);
        return page.map(sj -> jobService.mapToSummaryDto(sj.getJob(), seeker.getUser()));
    }
}
