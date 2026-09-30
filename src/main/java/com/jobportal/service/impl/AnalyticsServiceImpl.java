package com.jobportal.service.impl;

import com.jobportal.dto.response.DashboardStatsDto;
import com.jobportal.entity.JobSeeker;
import com.jobportal.entity.Recruiter;
import com.jobportal.entity.enums.ApplicationStatus;
import com.jobportal.entity.enums.JobStatus;
import com.jobportal.entity.enums.RoleName;
import com.jobportal.repository.*;
import com.jobportal.service.AnalyticsService;
import com.jobportal.service.JobService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class AnalyticsServiceImpl implements AnalyticsService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final SavedJobRepository savedJobRepository;
    private final CompanyRepository companyRepository;
    private final JobService jobService;

    public AnalyticsServiceImpl(UserRepository userRepository,
                                RoleRepository roleRepository,
                                JobRepository jobRepository,
                                ApplicationRepository applicationRepository,
                                SavedJobRepository savedJobRepository,
                                CompanyRepository companyRepository,
                                JobService jobService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.jobRepository = jobRepository;
        this.applicationRepository = applicationRepository;
        this.savedJobRepository = savedJobRepository;
        this.companyRepository = companyRepository;
        this.jobService = jobService;
    }

    @Override
    public DashboardStatsDto getSeekerStats(JobSeeker seeker) {
        DashboardStatsDto stats = new DashboardStatsDto();
        stats.setSeekerTotalApplications(applicationRepository.countByJobSeeker(seeker));
        stats.setSeekerUnderReview(applicationRepository.countByJobSeekerAndStatus(seeker, ApplicationStatus.UNDER_REVIEW));
        stats.setSeekerShortlisted(applicationRepository.countByJobSeekerAndStatus(seeker, ApplicationStatus.SHORTLISTED));
        stats.setSeekerSavedJobs(savedJobRepository.countByJobSeeker(seeker));
        stats.setSeekerRecommendedJobs(jobService.getRecommendedJobs(seeker, 10).size());
        return stats;
    }

    @Override
    public DashboardStatsDto getRecruiterStats(Recruiter recruiter) {
        DashboardStatsDto stats = new DashboardStatsDto();
        stats.setRecruiterTotalJobs(jobRepository.countByRecruiter(recruiter));
        stats.setRecruiterActiveJobs(jobRepository.countByRecruiterAndStatus(recruiter, JobStatus.APPROVED));
        stats.setRecruiterTotalApplicants(applicationRepository.countByRecruiterId(recruiter.getId()));
        stats.setRecruiterUnderReviewApplicants(applicationRepository.countByRecruiterIdAndStatus(recruiter.getId(), ApplicationStatus.UNDER_REVIEW));
        stats.setRecruiterShortlistedApplicants(applicationRepository.countByRecruiterIdAndStatus(recruiter.getId(), ApplicationStatus.SHORTLISTED));
        return stats;
    }

    @Override
    public DashboardStatsDto getAdminStats() {
        DashboardStatsDto stats = new DashboardStatsDto();
        stats.setAdminTotalUsers(userRepository.count());

        roleRepository.findByName(RoleName.ROLE_JOB_SEEKER).ifPresent(role ->
                stats.setAdminTotalSeekers(userRepository.countByRole(role)));

        roleRepository.findByName(RoleName.ROLE_RECRUITER).ifPresent(role ->
                stats.setAdminTotalRecruiters(userRepository.countByRole(role)));

        stats.setAdminTotalJobs(jobRepository.count());
        stats.setAdminActiveJobs(jobRepository.countByStatus(JobStatus.APPROVED));
        stats.setAdminPendingJobs(jobRepository.countByStatus(JobStatus.PENDING));
        stats.setAdminTotalApplications(applicationRepository.count());

        // Aggregate by status for Chart.js
        Map<String, Long> statusMap = new HashMap<>();
        List<Object[]> statusCounts = applicationRepository.countApplicationsByStatus();
        for (Object[] row : statusCounts) {
            if (row[0] != null) {
                statusMap.put(row[0].toString(), ((Number) row[1]).longValue());
            }
        }
        stats.setApplicationsByStatus(statusMap);

        // Aggregate by category for Chart.js
        Map<String, Long> catMap = new HashMap<>();
        List<Object[]> catCounts = jobRepository.countJobsByCategory(JobStatus.APPROVED);
        for (Object[] row : catCounts) {
            if (row[0] != null) {
                catMap.put(row[0].toString(), ((Number) row[1]).longValue());
            }
        }
        stats.setJobsByCategory(catMap);

        // Aggregate by company for Chart.js
        Map<String, Long> compMap = new HashMap<>();
        List<Object[]> compCounts = jobRepository.countJobsByCompany(JobStatus.APPROVED);
        for (Object[] row : compCounts) {
            if (row[0] != null) {
                compMap.put(row[0].toString(), ((Number) row[1]).longValue());
            }
        }
        stats.setJobsByCompany(compMap);

        return stats;
    }

    @Override
    public Map<String, Long> getPublicPlatformStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("totalJobs", jobRepository.countByStatus(JobStatus.APPROVED));
        stats.put("totalCompanies", companyRepository.count());
        roleRepository.findByName(RoleName.ROLE_JOB_SEEKER).ifPresent(role ->
                stats.put("totalSeekers", userRepository.countByRole(role)));
        stats.put("totalApplications", applicationRepository.count());
        return stats;
    }
}
