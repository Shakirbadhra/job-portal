package com.jobportal.controller.page;

import com.jobportal.dto.response.DashboardStatsDto;
import com.jobportal.dto.response.JobSummaryDto;
import com.jobportal.dto.response.UserProfileDto;
import com.jobportal.entity.JobSeeker;
import com.jobportal.entity.User;
import com.jobportal.security.SecurityUtils;
import com.jobportal.service.AnalyticsService;
import com.jobportal.service.JobService;
import com.jobportal.service.ResumeService;
import com.jobportal.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/seeker")
public class SeekerPageController {

    private final UserService userService;
    private final JobService jobService;
    private final ResumeService resumeService;
    private final AnalyticsService analyticsService;
    private final SecurityUtils securityUtils;

    public SeekerPageController(UserService userService,
                                JobService jobService,
                                ResumeService resumeService,
                                AnalyticsService analyticsService,
                                SecurityUtils securityUtils) {
        this.userService = userService;
        this.jobService = jobService;
        this.resumeService = resumeService;
        this.analyticsService = analyticsService;
        this.securityUtils = securityUtils;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        User user = securityUtils.getCurrentUserOrThrow();
        JobSeeker seeker = userService.getJobSeeker(user);
        DashboardStatsDto stats = analyticsService.getSeekerStats(seeker);
        List<JobSummaryDto> recommendedJobs = jobService.getRecommendedJobs(seeker, 6);

        model.addAttribute("currentUser", user);
        model.addAttribute("stats", stats);
        model.addAttribute("recommendedJobs", recommendedJobs);
        return "seeker/dashboard";
    }

    @GetMapping("/profile")
    public String profile(Model model) {
        User user = securityUtils.getCurrentUserOrThrow();
        UserProfileDto profile = userService.getUserProfile(user);
        model.addAttribute("currentUser", user);
        model.addAttribute("profile", profile);
        return "seeker/profile";
    }

    @GetMapping("/resumes")
    public String resumes(Model model) {
        User user = securityUtils.getCurrentUserOrThrow();
        JobSeeker seeker = userService.getJobSeeker(user);
        model.addAttribute("currentUser", user);
        model.addAttribute("resumes", resumeService.getSeekerResumes(seeker));
        return "seeker/resumes";
    }

    @GetMapping("/applied-jobs")
    public String appliedJobs(Model model) {
        User user = securityUtils.getCurrentUserOrThrow();
        model.addAttribute("currentUser", user);
        return "seeker/applied-jobs";
    }

    @GetMapping("/saved-jobs")
    public String savedJobs(Model model) {
        User user = securityUtils.getCurrentUserOrThrow();
        model.addAttribute("currentUser", user);
        return "seeker/saved-jobs";
    }
}
