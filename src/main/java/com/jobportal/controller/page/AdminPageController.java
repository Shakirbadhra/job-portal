package com.jobportal.controller.page;

import com.jobportal.dto.response.DashboardStatsDto;
import com.jobportal.entity.User;
import com.jobportal.security.SecurityUtils;
import com.jobportal.service.AnalyticsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminPageController {

    private final AnalyticsService analyticsService;
    private final SecurityUtils securityUtils;

    public AdminPageController(AnalyticsService analyticsService, SecurityUtils securityUtils) {
        this.analyticsService = analyticsService;
        this.securityUtils = securityUtils;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        User user = securityUtils.getCurrentUserOrThrow();
        DashboardStatsDto stats = analyticsService.getAdminStats();
        model.addAttribute("currentUser", user);
        model.addAttribute("stats", stats);
        return "admin/dashboard";
    }

    @GetMapping("/manage-users")
    public String manageUsers(Model model) {
        User user = securityUtils.getCurrentUserOrThrow();
        model.addAttribute("currentUser", user);
        return "admin/manage-users";
    }

    @GetMapping("/manage-jobs")
    public String manageJobs(Model model) {
        User user = securityUtils.getCurrentUserOrThrow();
        model.addAttribute("currentUser", user);
        return "admin/manage-jobs";
    }

    @GetMapping("/manage-applications")
    public String manageApplications(Model model) {
        User user = securityUtils.getCurrentUserOrThrow();
        model.addAttribute("currentUser", user);
        return "admin/manage-applications";
    }

    @GetMapping("/messages")
    public String messages(Model model) {
        User user = securityUtils.getCurrentUserOrThrow();
        model.addAttribute("currentUser", user);
        return "admin/messages";
    }
}
