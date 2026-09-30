package com.jobportal.dto.response;

import java.util.HashMap;
import java.util.Map;

public class DashboardStatsDto {

    // Seeker specific stats
    private long seekerTotalApplications;
    private long seekerUnderReview;
    private long seekerShortlisted;
    private long seekerSavedJobs;
    private long seekerRecommendedJobs;

    // Recruiter specific stats
    private long recruiterTotalJobs;
    private long recruiterActiveJobs;
    private long recruiterTotalApplicants;
    private long recruiterUnderReviewApplicants;
    private long recruiterShortlistedApplicants;

    // Admin specific stats
    private long adminTotalUsers;
    private long adminTotalSeekers;
    private long adminTotalRecruiters;
    private long adminTotalJobs;
    private long adminActiveJobs;
    private long adminPendingJobs;
    private long adminTotalApplications;

    // Chart data
    private Map<String, Long> applicationsByStatus = new HashMap<>();
    private Map<String, Long> jobsByCategory = new HashMap<>();
    private Map<String, Long> jobsByCompany = new HashMap<>();

    public DashboardStatsDto() {}

    public long getSeekerTotalApplications() {
        return seekerTotalApplications;
    }

    public void setSeekerTotalApplications(long seekerTotalApplications) {
        this.seekerTotalApplications = seekerTotalApplications;
    }

    public long getSeekerUnderReview() {
        return seekerUnderReview;
    }

    public void setSeekerUnderReview(long seekerUnderReview) {
        this.seekerUnderReview = seekerUnderReview;
    }

    public long getSeekerShortlisted() {
        return seekerShortlisted;
    }

    public void setSeekerShortlisted(long seekerShortlisted) {
        this.seekerShortlisted = seekerShortlisted;
    }

    public long getSeekerSavedJobs() {
        return seekerSavedJobs;
    }

    public void setSeekerSavedJobs(long seekerSavedJobs) {
        this.seekerSavedJobs = seekerSavedJobs;
    }

    public long getSeekerRecommendedJobs() {
        return seekerRecommendedJobs;
    }

    public void setSeekerRecommendedJobs(long seekerRecommendedJobs) {
        this.seekerRecommendedJobs = seekerRecommendedJobs;
    }

    public long getRecruiterTotalJobs() {
        return recruiterTotalJobs;
    }

    public void setRecruiterTotalJobs(long recruiterTotalJobs) {
        this.recruiterTotalJobs = recruiterTotalJobs;
    }

    public long getRecruiterActiveJobs() {
        return recruiterActiveJobs;
    }

    public void setRecruiterActiveJobs(long recruiterActiveJobs) {
        this.recruiterActiveJobs = recruiterActiveJobs;
    }

    public long getRecruiterTotalApplicants() {
        return recruiterTotalApplicants;
    }

    public void setRecruiterTotalApplicants(long recruiterTotalApplicants) {
        this.recruiterTotalApplicants = recruiterTotalApplicants;
    }

    public long getRecruiterUnderReviewApplicants() {
        return recruiterUnderReviewApplicants;
    }

    public void setRecruiterUnderReviewApplicants(long recruiterUnderReviewApplicants) {
        this.recruiterUnderReviewApplicants = recruiterUnderReviewApplicants;
    }

    public long getRecruiterShortlistedApplicants() {
        return recruiterShortlistedApplicants;
    }

    public void setRecruiterShortlistedApplicants(long recruiterShortlistedApplicants) {
        this.recruiterShortlistedApplicants = recruiterShortlistedApplicants;
    }

    public long getAdminTotalUsers() {
        return adminTotalUsers;
    }

    public void setAdminTotalUsers(long adminTotalUsers) {
        this.adminTotalUsers = adminTotalUsers;
    }

    public long getAdminTotalSeekers() {
        return adminTotalSeekers;
    }

    public void setAdminTotalSeekers(long adminTotalSeekers) {
        this.adminTotalSeekers = adminTotalSeekers;
    }

    public long getAdminTotalRecruiters() {
        return adminTotalRecruiters;
    }

    public void setAdminTotalRecruiters(long adminTotalRecruiters) {
        this.adminTotalRecruiters = adminTotalRecruiters;
    }

    public long getAdminTotalJobs() {
        return adminTotalJobs;
    }

    public void setAdminTotalJobs(long adminTotalJobs) {
        this.adminTotalJobs = adminTotalJobs;
    }

    public long getAdminActiveJobs() {
        return adminActiveJobs;
    }

    public void setAdminActiveJobs(long adminActiveJobs) {
        this.adminActiveJobs = adminActiveJobs;
    }

    public long getAdminPendingJobs() {
        return adminPendingJobs;
    }

    public void setAdminPendingJobs(long adminPendingJobs) {
        this.adminPendingJobs = adminPendingJobs;
    }

    public long getAdminTotalApplications() {
        return adminTotalApplications;
    }

    public void setAdminTotalApplications(long adminTotalApplications) {
        this.adminTotalApplications = adminTotalApplications;
    }

    public Map<String, Long> getApplicationsByStatus() {
        return applicationsByStatus;
    }

    public void setApplicationsByStatus(Map<String, Long> applicationsByStatus) {
        this.applicationsByStatus = applicationsByStatus;
    }

    public Map<String, Long> getJobsByCategory() {
        return jobsByCategory;
    }

    public void setJobsByCategory(Map<String, Long> jobsByCategory) {
        this.jobsByCategory = jobsByCategory;
    }

    public Map<String, Long> getJobsByCompany() {
        return jobsByCompany;
    }

    public void setJobsByCompany(Map<String, Long> jobsByCompany) {
        this.jobsByCompany = jobsByCompany;
    }
}
