package com.jobportal.controller.page;

import com.jobportal.dto.response.ApplicationResponseDto;
import com.jobportal.dto.response.DashboardStatsDto;
import com.jobportal.dto.response.JobDetailDto;
import com.jobportal.dto.response.UserProfileDto;
import com.jobportal.entity.Recruiter;
import com.jobportal.entity.User;
import com.jobportal.security.SecurityUtils;
import com.jobportal.service.AnalyticsService;
import com.jobportal.service.ApplicationService;
import com.jobportal.service.JobService;
import com.jobportal.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/recruiter")
public class RecruiterPageController {

    private final UserService userService;
    private final JobService jobService;
    private final ApplicationService applicationService;
    private final AnalyticsService analyticsService;
    private final SecurityUtils securityUtils;

    public RecruiterPageController(UserService userService,
                                   JobService jobService,
                                   ApplicationService applicationService,
                                   AnalyticsService analyticsService,
                                   SecurityUtils securityUtils) {
        this.userService = userService;
        this.jobService = jobService;
        this.applicationService = applicationService;
        this.analyticsService = analyticsService;
        this.securityUtils = securityUtils;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        User user = securityUtils.getCurrentUserOrThrow();
        Recruiter recruiter = userService.getRecruiter(user);
        DashboardStatsDto stats = analyticsService.getRecruiterStats(recruiter);

        model.addAttribute("currentUser", user);
        model.addAttribute("stats", stats);
        model.addAttribute("company", recruiter.getCompany());
        return "recruiter/dashboard";
    }

    @GetMapping("/company-profile")
    public String companyProfile(Model model) {
        User user = securityUtils.getCurrentUserOrThrow();
        Recruiter recruiter = userService.getRecruiter(user);
        UserProfileDto profile = userService.getUserProfile(user);

        model.addAttribute("currentUser", user);
        model.addAttribute("recruiter", recruiter);
        model.addAttribute("company", recruiter.getCompany());
        model.addAttribute("profile", profile);
        return "recruiter/company-profile";
    }

    @GetMapping("/post-job")
    public String postJob(Model model) {
        User user = securityUtils.getCurrentUserOrThrow();
        Recruiter recruiter = userService.getRecruiter(user);
        List<String> categories = jobService.getCategories();

        model.addAttribute("currentUser", user);
        model.addAttribute("company", recruiter.getCompany());
        model.addAttribute("categories", categories);
        return "recruiter/post-job";
    }

    @GetMapping("/edit-job/{id}")
    public String editJob(@PathVariable Long id, Model model) {
        User user = securityUtils.getCurrentUserOrThrow();
        Recruiter recruiter = userService.getRecruiter(user);
        JobDetailDto job = jobService.getJobForRecruiter(id, recruiter);
        List<String> categories = jobService.getCategories();

        model.addAttribute("currentUser", user);
        model.addAttribute("job", job);
        model.addAttribute("categories", categories);
        return "recruiter/edit-job";
    }

    @GetMapping("/manage-jobs")
    public String manageJobs(Model model) {
        User user = securityUtils.getCurrentUserOrThrow();
        model.addAttribute("currentUser", user);
        return "recruiter/manage-jobs";
    }

    @GetMapping("/applicants")
    public String applicants(Model model) {
        User user = securityUtils.getCurrentUserOrThrow();
        model.addAttribute("currentUser", user);
        return "recruiter/applicants";
    }

    @GetMapping("/applicants/{id}")
    public String applicantDetails(@PathVariable Long id, Model model) {
        User user = securityUtils.getCurrentUserOrThrow();
        ApplicationResponseDto application = applicationService.getApplicationDetails(id, user);

        model.addAttribute("currentUser", user);
        model.addAttribute("app", application);
        return "recruiter/applicant-details";
    }
}
