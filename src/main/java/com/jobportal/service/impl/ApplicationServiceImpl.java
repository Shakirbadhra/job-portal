package com.jobportal.service.impl;

import com.jobportal.dto.request.ApplicationStatusUpdateRequest;
import com.jobportal.dto.request.ApplicationSubmitRequest;
import com.jobportal.dto.response.ApplicationResponseDto;
import com.jobportal.entity.*;
import com.jobportal.entity.enums.ApplicationStatus;
import com.jobportal.entity.enums.JobStatus;
import com.jobportal.exception.BadRequestException;
import com.jobportal.exception.DuplicateResourceException;
import com.jobportal.exception.ResourceNotFoundException;
import com.jobportal.exception.UnauthorizedException;
import com.jobportal.repository.ApplicationRepository;
import com.jobportal.repository.JobRepository;
import com.jobportal.repository.ResumeRepository;
import com.jobportal.service.ApplicationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final ResumeRepository resumeRepository;

    public ApplicationServiceImpl(ApplicationRepository applicationRepository,
                                  JobRepository jobRepository,
                                  ResumeRepository resumeRepository) {
        this.applicationRepository = applicationRepository;
        this.jobRepository = jobRepository;
        this.resumeRepository = resumeRepository;
    }

    @Override
    public ApplicationResponseDto apply(ApplicationSubmitRequest request, JobSeeker seeker) {
        Job job = jobRepository.findById(request.getJobId())
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + request.getJobId()));

        if (job.getStatus() != JobStatus.APPROVED) {
            throw new BadRequestException("This job is no longer active for applications");
        }

        if (applicationRepository.existsByJobAndJobSeeker(job, seeker)) {
            throw new DuplicateResourceException("You have already applied for this job");
        }

        Resume resume = null;
        if (request.getResumeId() != null) {
            resume = resumeRepository.findById(request.getResumeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Resume not found with id: " + request.getResumeId()));
            if (!resume.getJobSeeker().getId().equals(seeker.getId())) {
                throw new BadRequestException("You do not own this resume");
            }
        } else {
            // Find seeker's primary resume
            resume = resumeRepository.findByJobSeekerAndIsPrimaryTrue(seeker)
                    .orElse(null);
            if (resume == null) {
                // Pick the most recent resume
                java.util.List<Resume> resumes = resumeRepository.findByJobSeekerOrderByUploadedAtDesc(seeker);
                if (!resumes.isEmpty()) {
                    resume = resumes.get(0);
                }
            }
        }

        if (resume == null) {
            throw new BadRequestException("Please upload a resume first before applying");
        }

        Application application = new Application();
        application.setJob(job);
        application.setJobSeeker(seeker);
        application.setResume(resume);
        if (StringUtils.hasText(request.getCoverLetter())) {
            application.setCoverLetter(request.getCoverLetter().trim());
        }
        application.setStatus(ApplicationStatus.APPLIED);

        application = applicationRepository.save(application);
        return mapToDto(application);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasApplied(Long jobId, JobSeeker seeker) {
        Job job = jobRepository.findById(jobId).orElse(null);
        if (job == null) return false;
        return applicationRepository.existsByJobAndJobSeeker(job, seeker);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ApplicationResponseDto> getSeekerApplications(JobSeeker seeker, Pageable pageable) {
        Page<Application> page = applicationRepository.findByJobSeekerOrderByAppliedAtDesc(seeker, pageable);
        return page.map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ApplicationResponseDto> getRecruiterApplications(Long recruiterId, Pageable pageable) {
        Page<Application> page = applicationRepository.findByRecruiterId(recruiterId, pageable);
        return page.map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ApplicationResponseDto> getJobApplications(Long jobId, Recruiter recruiter, Pageable pageable) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + jobId));

        if (!job.getRecruiter().getId().equals(recruiter.getId())) {
            throw new UnauthorizedException("You can only view applicants for your own posted jobs");
        }

        Page<Application> page = applicationRepository.findByJobOrderByAppliedAtDesc(job, pageable);
        return page.map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationResponseDto getApplicationDetails(Long applicationId) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + applicationId));
        return mapToDto(app);
    }

    @Override
    public ApplicationResponseDto updateApplicationStatus(Long applicationId, ApplicationStatusUpdateRequest request, Recruiter recruiter) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + applicationId));

        if (recruiter != null && !application.getJob().getRecruiter().getId().equals(recruiter.getId())) {
            throw new UnauthorizedException("You can only modify applications for your own posted jobs");
        }

        application.setStatus(request.getStatus());
        if (request.getRecruiterNotes() != null) {
            application.setRecruiterNotes(request.getRecruiterNotes().trim());
        }

        application = applicationRepository.save(application);
        return mapToDto(application);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ApplicationResponseDto> getAllApplicationsForAdmin(Pageable pageable) {
        Page<Application> page = applicationRepository.findAll(pageable);
        return page.map(this::mapToDto);
    }

    private ApplicationResponseDto mapToDto(Application a) {
        ApplicationResponseDto dto = new ApplicationResponseDto();
        dto.setId(a.getId());
        dto.setJobId(a.getJob().getId());
        dto.setJobTitle(a.getJob().getTitle());
        dto.setJobLocation(a.getJob().getLocation());
        dto.setEmploymentType(a.getJob().getEmploymentType().getDisplayName());

        if (a.getJob().getCompany() != null) {
            dto.setCompanyName(a.getJob().getCompany().getName());
            dto.setCompanyLogoUrl(a.getJob().getCompany().getLogoUrl());
        }

        JobSeeker seeker = a.getJobSeeker();
        dto.setSeekerId(seeker.getId());
        dto.setUserId(seeker.getUser().getId());
        dto.setSeekerName(seeker.getUser().getFullName());
        dto.setSeekerEmail(seeker.getUser().getEmail());
        dto.setSeekerPhone(seeker.getUser().getPhone());
        dto.setSeekerHeadline(seeker.getHeadline());
        dto.setSeekerExperience(seeker.getTotalExperienceYears());
        dto.setSeekerQualification(seeker.getHighestQualification());

        if (a.getResume() != null) {
            dto.setResumeId(a.getResume().getId());
            dto.setResumeFilename(a.getResume().getOriginalFilename());
        }

        dto.setCoverLetter(a.getCoverLetter());
        dto.setStatus(a.getStatus().name());
        dto.setStatusDisplayName(a.getStatus().getDisplayName());
        dto.setBadgeClass(a.getStatus().getBadgeClass());
        dto.setRecruiterNotes(a.getRecruiterNotes());
        dto.setAppliedAt(a.getAppliedAt());
        dto.setUpdatedAt(a.getUpdatedAt());
        return dto;
    }
}
