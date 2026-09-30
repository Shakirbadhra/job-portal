package com.jobportal.service.impl;

import com.jobportal.dto.request.JobCreateUpdateRequest;
import com.jobportal.dto.request.JobSearchCriteria;
import com.jobportal.dto.response.JobDetailDto;
import com.jobportal.dto.response.JobSummaryDto;
import com.jobportal.entity.*;
import com.jobportal.entity.enums.JobStatus;
import com.jobportal.exception.BadRequestException;
import com.jobportal.exception.ResourceNotFoundException;
import com.jobportal.exception.UnauthorizedException;
import com.jobportal.repository.*;
import com.jobportal.repository.specification.JobSpecification;
import com.jobportal.service.JobService;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;
    private final SkillRepository skillRepository;
    private final ApplicationRepository applicationRepository;
    private final SavedJobRepository savedJobRepository;
    private final JobSeekerRepository jobSeekerRepository;

    public JobServiceImpl(JobRepository jobRepository,
                          SkillRepository skillRepository,
                          ApplicationRepository applicationRepository,
                          SavedJobRepository savedJobRepository,
                          JobSeekerRepository jobSeekerRepository) {
        this.jobRepository = jobRepository;
        this.skillRepository = skillRepository;
        this.applicationRepository = applicationRepository;
        this.savedJobRepository = savedJobRepository;
        this.jobSeekerRepository = jobSeekerRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JobSummaryDto> searchJobs(JobSearchCriteria criteria, Pageable pageable, User currentUser) {
        Page<Job> jobs = jobRepository.findAll(JobSpecification.build(criteria), pageable);
        return jobs.map(j -> mapToSummaryDto(j, currentUser));
    }

    @Override
    public JobDetailDto getJobDetails(Long id, User currentUser) {
        Job job = getJobEntity(id);
        // Increment views count
        job.setViewsCount(job.getViewsCount() + 1);
        jobRepository.save(job);
        return mapToDetailDto(job, currentUser);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobSummaryDto> getFeaturedJobs(User currentUser) {
        return jobRepository.findTop6ByStatusAndIsFeaturedTrueOrderByCreatedAtDesc(JobStatus.APPROVED)
                .stream()
                .map(j -> mapToSummaryDto(j, currentUser))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobSummaryDto> getRecentJobs(User currentUser) {
        return jobRepository.findTop8ByStatusOrderByCreatedAtDesc(JobStatus.APPROVED)
                .stream()
                .map(j -> mapToSummaryDto(j, currentUser))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getCategories() {
        return jobRepository.findDistinctJobCategories(JobStatus.APPROVED);
    }

    @Override
    public JobDetailDto createJob(JobCreateUpdateRequest request, Recruiter recruiter) {
        if (recruiter.getCompany() == null) {
            throw new BadRequestException("Recruiter must be linked to a company to post jobs");
        }

        Job job = new Job();
        job.setRecruiter(recruiter);
        job.setCompany(recruiter.getCompany());
        applyJobData(job, request);
        job.setStatus(JobStatus.APPROVED); // Default approved or pending

        job = jobRepository.save(job);
        return mapToDetailDto(job, recruiter.getUser());
    }

    @Override
    public JobDetailDto updateJob(Long jobId, JobCreateUpdateRequest request, Recruiter recruiter) {
        Job job = getJobEntity(jobId);
        if (!job.getRecruiter().getId().equals(recruiter.getId())) {
            throw new UnauthorizedException("You can only edit your own posted jobs");
        }

        applyJobData(job, request);
        job = jobRepository.save(job);
        return mapToDetailDto(job, recruiter.getUser());
    }

    @Override
    public void deleteJob(Long jobId, Recruiter recruiter) {
        Job job = getJobEntity(jobId);
        if (!job.getRecruiter().getId().equals(recruiter.getId())) {
            throw new UnauthorizedException("You can only delete your own posted jobs");
        }
        jobRepository.delete(job);
    }

    @Override
    public void deleteJobByAdmin(Long jobId) {
        Job job = getJobEntity(jobId);
        jobRepository.delete(job);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JobSummaryDto> getRecruiterJobs(Recruiter recruiter, Pageable pageable) {
        Page<Job> jobs = jobRepository.findByRecruiter(recruiter, pageable);
        return jobs.map(j -> mapToSummaryDto(j, recruiter.getUser()));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JobDetailDto> getAllJobsForAdmin(JobStatus status, Pageable pageable) {
        Page<Job> jobs;
        if (status != null) {
            jobs = jobRepository.findByStatus(status, pageable);
        } else {
            jobs = jobRepository.findAll(pageable);
        }
        return jobs.map(j -> mapToDetailDto(j, null));
    }

    @Override
    public boolean approveJob(Long jobId) {
        Job job = getJobEntity(jobId);
        job.setStatus(JobStatus.APPROVED);
        jobRepository.save(job);
        return true;
    }

    @Override
    public boolean rejectJob(Long jobId) {
        Job job = getJobEntity(jobId);
        job.setStatus(JobStatus.REJECTED);
        jobRepository.save(job);
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobSummaryDto> getRecommendedJobs(JobSeeker seeker, int limit) {
        Set<Skill> skills = seeker.getSkills();
        if (skills.isEmpty()) {
            return getRecentJobs(seeker.getUser()).stream().limit(limit).collect(Collectors.toList());
        }

        List<Job> approvedJobs = jobRepository.findByStatus(JobStatus.APPROVED, PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent();

        // Sort by number of matching skills
        return approvedJobs.stream()
                .sorted((j1, j2) -> {
                    long match1 = j1.getSkills().stream().filter(skills::contains).count();
                    long match2 = j2.getSkills().stream().filter(skills::contains).count();
                    return Long.compare(match2, match1);
                })
                .limit(limit)
                .map(j -> mapToSummaryDto(j, seeker.getUser()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Job getJobEntity(Long jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + jobId));
    }

    private void applyJobData(Job job, JobCreateUpdateRequest req) {
        job.setTitle(req.getTitle().trim());
        job.setJobCategory(req.getJobCategory().trim());
        job.setEmploymentType(req.getEmploymentType());
        job.setWorkplaceType(req.getWorkplaceType());
        job.setExperienceLevel(req.getExperienceLevel());
        job.setMinExperienceYears(req.getMinExperienceYears());
        job.setMaxExperienceYears(req.getMaxExperienceYears());
        job.setMinSalary(req.getMinSalary());
        job.setMaxSalary(req.getMaxSalary());
        job.setLocation(req.getLocation().trim());
        job.setDescription(req.getDescription().trim());
        job.setResponsibilities(req.getResponsibilities() != null ? req.getResponsibilities().trim() : null);
        job.setRequirements(req.getRequirements() != null ? req.getRequirements().trim() : null);
        job.setBenefits(req.getBenefits() != null ? req.getBenefits().trim() : null);
        job.setDeadline(req.getDeadline());
        job.setFeatured(req.isFeatured());

        if (req.getSkills() != null) {
            Set<Skill> skillEntities = new HashSet<>();
            for (String s : req.getSkills()) {
                if (StringUtils.hasText(s)) {
                    String clean = s.trim();
                    Skill skill = skillRepository.findByNameIgnoreCase(clean)
                            .orElseGet(() -> skillRepository.save(new Skill(clean)));
                    skillEntities.add(skill);
                }
            }
            job.setSkills(skillEntities);
        }
    }

    public JobSummaryDto mapToSummaryDto(Job job, User currentUser) {
        JobSummaryDto dto = new JobSummaryDto();
        dto.setId(job.getId());
        dto.setTitle(job.getTitle());
        dto.setSlug(job.getSlug());
        if (job.getCompany() != null) {
            dto.setCompanyId(job.getCompany().getId());
            dto.setCompanyName(job.getCompany().getName());
            dto.setCompanyLogoUrl(job.getCompany().getLogoUrl());
        }
        dto.setJobCategory(job.getJobCategory());
        dto.setEmploymentType(job.getEmploymentType().getDisplayName());
        dto.setWorkplaceType(job.getWorkplaceType().getDisplayName());
        dto.setExperienceLevel(job.getExperienceLevel().getDisplayName());
        dto.setMinExperienceYears(job.getMinExperienceYears());
        dto.setMaxExperienceYears(job.getMaxExperienceYears());
        dto.setMinSalary(job.getMinSalary());
        dto.setMaxSalary(job.getMaxSalary());
        dto.setLocation(job.getLocation());
        dto.setFeatured(job.isFeatured());
        dto.setPostedTimeAgo(formatTimeAgo(job.getCreatedAt()));
        dto.setSkills(job.getSkills().stream().map(Skill::getName).collect(Collectors.toList()));

        if (currentUser != null) {
            jobSeekerRepository.findByUserId(currentUser.getId()).ifPresent(seeker -> {
                dto.setSaved(savedJobRepository.existsByJobSeekerAndJob(seeker, job));
                dto.setHasApplied(applicationRepository.existsByJobAndJobSeeker(job, seeker));
            });
        }
        return dto;
    }

    public JobDetailDto mapToDetailDto(Job job, User currentUser) {
        JobDetailDto dto = new JobDetailDto();
        dto.setId(job.getId());
        dto.setTitle(job.getTitle());
        dto.setSlug(job.getSlug());
        if (job.getCompany() != null) {
            dto.setCompanyId(job.getCompany().getId());
            dto.setCompanyName(job.getCompany().getName());
            dto.setCompanyLogoUrl(job.getCompany().getLogoUrl());
            dto.setCompanyWebsite(job.getCompany().getWebsite());
            dto.setCompanyIndustry(job.getCompany().getIndustry());
            dto.setCompanyDescription(job.getCompany().getDescription());
            dto.setCompanySize(job.getCompany().getCompanySize());
            dto.setCompanyFoundedYear(job.getCompany().getFoundedYear());
        }
        dto.setJobCategory(job.getJobCategory());
        dto.setEmploymentType(job.getEmploymentType().getDisplayName());
        dto.setWorkplaceType(job.getWorkplaceType().getDisplayName());
        dto.setExperienceLevel(job.getExperienceLevel().getDisplayName());
        dto.setMinExperienceYears(job.getMinExperienceYears());
        dto.setMaxExperienceYears(job.getMaxExperienceYears());
        dto.setMinSalary(job.getMinSalary());
        dto.setMaxSalary(job.getMaxSalary());
        dto.setLocation(job.getLocation());
        dto.setDescription(job.getDescription());
        dto.setResponsibilities(job.getResponsibilities());
        dto.setRequirements(job.getRequirements());
        dto.setBenefits(job.getBenefits());
        dto.setStatus(job.getStatus().name());
        dto.setFeatured(job.isFeatured());
        dto.setViewsCount(job.getViewsCount());
        dto.setDeadline(job.getDeadline());
        dto.setPostedTimeAgo(formatTimeAgo(job.getCreatedAt()));
        dto.setSkills(job.getSkills().stream().map(Skill::getName).collect(Collectors.toList()));

        if (job.getRecruiter() != null) {
            dto.setRecruiterName(job.getRecruiter().getUser().getFullName());
            dto.setRecruiterDesignation(job.getRecruiter().getDesignation());
        }

        dto.setApplicationCount((long) job.getApplications().size());

        if (currentUser != null) {
            jobSeekerRepository.findByUserId(currentUser.getId()).ifPresent(seeker -> {
                dto.setSaved(savedJobRepository.existsByJobSeekerAndJob(seeker, job));
                dto.setHasApplied(applicationRepository.existsByJobAndJobSeeker(job, seeker));
            });
        }
        return dto;
    }

    private String formatTimeAgo(LocalDateTime dateTime) {
        if (dateTime == null) return "Recently";
        Duration duration = Duration.between(dateTime, LocalDateTime.now());
        long seconds = duration.getSeconds();
        if (seconds < 60) return "Just now";
        long minutes = seconds / 60;
        if (minutes < 60) return minutes + "m ago";
        long hours = minutes / 60;
        if (hours < 24) return hours + "h ago";
        long days = hours / 24;
        if (days < 30) return days + "d ago";
        return (days / 30) + "mo ago";
    }
}
