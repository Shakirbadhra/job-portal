package com.jobportal.service;

import com.jobportal.dto.response.DashboardStatsDto;
import com.jobportal.entity.JobSeeker;
import com.jobportal.entity.Recruiter;

import java.util.Map;

public interface AnalyticsService {
    DashboardStatsDto getSeekerStats(JobSeeker seeker);
    DashboardStatsDto getRecruiterStats(Recruiter recruiter);
    DashboardStatsDto getAdminStats();
    Map<String, Long> getPublicPlatformStats();
}
