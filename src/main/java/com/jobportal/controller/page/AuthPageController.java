package com.jobportal.controller.page;

import com.jobportal.security.SecurityUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthPageController {

    private final SecurityUtils securityUtils;

    public AuthPageController(SecurityUtils securityUtils) {
        this.securityUtils = securityUtils;
    }

    @GetMapping("/login")
    public String login(@RequestParam(value = "error", required = false) String error,
                        @RequestParam(value = "logout", required = false) String logout,
                        Model model) {
        if (securityUtils.getCurrentUser().isPresent()) {
            return "redirect:/";
        }
        if (error != null) {
            model.addAttribute("errorMessage", "Invalid email or password. Please try again.");
        }
        if (logout != null) {
            model.addAttribute("successMessage", "You have been logged out successfully.");
        }
        return "public/login";
    }

    @GetMapping("/register")
    public String register(Model model) {
        if (securityUtils.getCurrentUser().isPresent()) {
            return "redirect:/";
        }
        return "public/register";
    }
}
