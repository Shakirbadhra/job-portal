package com.jobportal.controller.page;

import com.jobportal.dto.response.CompanyDto;
import com.jobportal.dto.response.JobSummaryDto;
import com.jobportal.entity.User;
import com.jobportal.security.SecurityUtils;
import com.jobportal.service.AnalyticsService;
import com.jobportal.service.CompanyService;
import com.jobportal.service.JobService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

@Controller
public class HomeController {

    private final JobService jobService;
    private final CompanyService companyService;
    private final AnalyticsService analyticsService;
    private final SecurityUtils securityUtils;

    public HomeController(JobService jobService,
                          CompanyService companyService,
                          AnalyticsService analyticsService,
                          SecurityUtils securityUtils) {
        this.jobService = jobService;
        this.companyService = companyService;
        this.analyticsService = analyticsService;
        this.securityUtils = securityUtils;
    }

    @GetMapping("/")
    public String index(Model model) {
        User currentUser = securityUtils.getCurrentUser().orElse(null);
        List<JobSummaryDto> featuredJobs = jobService.getFeaturedJobs(currentUser);
        List<JobSummaryDto> recentJobs = jobService.getRecentJobs(currentUser);
        List<CompanyDto> topCompanies = companyService.getAllCompanies().stream().limit(8).collect(java.util.stream.Collectors.toList());
        List<String> categories = jobService.getCategories();
        Map<String, Long> stats = analyticsService.getPublicPlatformStats();

        model.addAttribute("featuredJobs", featuredJobs);
        model.addAttribute("recentJobs", recentJobs);
        model.addAttribute("topCompanies", topCompanies);
        model.addAttribute("categories", categories);
        model.addAttribute("categoryCounts", jobService.getCategoryCounts());
        model.addAttribute("stats", stats);
        model.addAttribute("currentUser", currentUser);
        return "public/index";
    }

    @GetMapping("/about")
    public String about(Model model) {
        model.addAttribute("currentUser", securityUtils.getCurrentUser().orElse(null));
        return "public/about";
    }

    @GetMapping("/contact")
    public String contact(Model model) {
        model.addAttribute("currentUser", securityUtils.getCurrentUser().orElse(null));
        return "public/contact";
    }
}
