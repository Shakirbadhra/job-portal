package com.jobportal.controller.page;

import com.jobportal.dto.response.CompanyDto;
import com.jobportal.dto.response.JobDetailDto;
import com.jobportal.entity.JobSeeker;
import com.jobportal.entity.User;
import com.jobportal.entity.enums.RoleName;
import com.jobportal.security.SecurityUtils;
import com.jobportal.service.CompanyService;
import com.jobportal.service.JobService;
import com.jobportal.service.ResumeService;
import com.jobportal.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class JobPageController {

    private final JobService jobService;
    private final CompanyService companyService;
    private final UserService userService;
    private final ResumeService resumeService;
    private final SecurityUtils securityUtils;

    public JobPageController(JobService jobService,
                             CompanyService companyService,
                             UserService userService,
                             ResumeService resumeService,
                             SecurityUtils securityUtils) {
        this.jobService = jobService;
        this.companyService = companyService;
        this.userService = userService;
        this.resumeService = resumeService;
        this.securityUtils = securityUtils;
    }

    @GetMapping("/jobs")
    public String jobsPage(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String employmentType,
            @RequestParam(required = false) String workplaceType,
            @RequestParam(required = false) String experienceLevel,
            Model model) {

        User currentUser = securityUtils.getCurrentUser().orElse(null);
        List<String> categories = jobService.getCategories();
        List<CompanyDto> companies = companyService.getAllCompanies();

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("categories", categories);
        model.addAttribute("companies", companies);
        model.addAttribute("initKeyword", keyword != null ? keyword : "");
        model.addAttribute("initLocation", location != null ? location : "");
        model.addAttribute("initCategory", category != null ? category : "");
        model.addAttribute("initEmploymentType", employmentType != null ? employmentType : "");
        model.addAttribute("initWorkplaceType", workplaceType != null ? workplaceType : "");
        model.addAttribute("initExperienceLevel", experienceLevel != null ? experienceLevel : "");
        return "public/jobs";
    }

    @GetMapping("/jobs/{id}")
    public String jobDetailsPage(@PathVariable Long id, Model model) {
        User currentUser = securityUtils.getCurrentUser().orElse(null);
        JobDetailDto job = jobService.getJobDetails(id, currentUser);
        model.addAttribute("job", job);
        model.addAttribute("currentUser", currentUser);

        if (currentUser != null && currentUser.getRole().getName() == RoleName.ROLE_JOB_SEEKER) {
            JobSeeker seeker = userService.getJobSeeker(currentUser);
            model.addAttribute("resumes", resumeService.getSeekerResumes(seeker));
        }

        return "public/job-details";
    }

    @GetMapping("/companies")
    public String companiesPage(Model model) {
        model.addAttribute("currentUser", securityUtils.getCurrentUser().orElse(null));
        model.addAttribute("companies", companyService.getAllCompanies());
        return "public/companies";
    }

    @GetMapping("/companies/{id}")
    public String companyDetailsPage(@PathVariable Long id, Model model) {
        User currentUser = securityUtils.getCurrentUser().orElse(null);
        CompanyDto company = companyService.getCompanyById(id);
        model.addAttribute("company", company);
        model.addAttribute("currentUser", currentUser);
        return "public/company-details";
    }
}
