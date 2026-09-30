package com.jobportal.controller.api;

import com.jobportal.dto.request.JobCreateUpdateRequest;
import com.jobportal.dto.request.JobSearchCriteria;
import com.jobportal.dto.response.ApiResponse;
import com.jobportal.dto.response.CompanyDto;
import com.jobportal.dto.response.JobDetailDto;
import com.jobportal.dto.response.JobSummaryDto;
import com.jobportal.entity.Recruiter;
import com.jobportal.entity.User;
import com.jobportal.entity.enums.EmploymentType;
import com.jobportal.entity.enums.ExperienceLevel;
import com.jobportal.entity.enums.WorkplaceType;
import com.jobportal.security.SecurityUtils;
import com.jobportal.service.CompanyService;
import com.jobportal.service.JobService;
import com.jobportal.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api")
public class JobRestController {

    private final JobService jobService;
    private final CompanyService companyService;
    private final UserService userService;
    private final SecurityUtils securityUtils;

    public JobRestController(JobService jobService,
                             CompanyService companyService,
                             UserService userService,
                             SecurityUtils securityUtils) {
        this.jobService = jobService;
        this.companyService = companyService;
        this.userService = userService;
        this.securityUtils = securityUtils;
    }

    @GetMapping("/jobs")
    public ResponseEntity<ApiResponse<Page<JobSummaryDto>>> searchJobs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) EmploymentType employmentType,
            @RequestParam(required = false) WorkplaceType workplaceType,
            @RequestParam(required = false) ExperienceLevel experienceLevel,
            @RequestParam(required = false) Integer minExperience,
            @RequestParam(required = false) BigDecimal minSalary,
            @RequestParam(required = false) BigDecimal maxSalary,
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) String skill,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {

        JobSearchCriteria criteria = new JobSearchCriteria();
        criteria.setKeyword(keyword);
        criteria.setLocation(location);
        criteria.setCategory(category);
        criteria.setEmploymentType(employmentType);
        criteria.setWorkplaceType(workplaceType);
        criteria.setExperienceLevel(experienceLevel);
        criteria.setMinExperience(minExperience);
        criteria.setMinSalary(minSalary);
        criteria.setMaxSalary(maxSalary);
        criteria.setCompanyId(companyId);
        criteria.setSkill(skill);

        String[] sortParts = sort.split(",");
        Sort.Direction direction = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("asc") ?
                Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortParts[0]));

        User currentUser = securityUtils.getCurrentUser().orElse(null);
        Page<JobSummaryDto> result = jobService.searchJobs(criteria, pageable, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Jobs fetched successfully", result));
    }

    @GetMapping("/jobs/{id}")
    public ResponseEntity<ApiResponse<JobDetailDto>> getJobDetails(@PathVariable Long id) {
        User currentUser = securityUtils.getCurrentUser().orElse(null);
        JobDetailDto job = jobService.getJobDetails(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Job details fetched", job));
    }

    @GetMapping("/jobs/featured")
    public ResponseEntity<ApiResponse<List<JobSummaryDto>>> getFeaturedJobs() {
        User currentUser = securityUtils.getCurrentUser().orElse(null);
        return ResponseEntity.ok(ApiResponse.success("Featured jobs fetched", jobService.getFeaturedJobs(currentUser)));
    }

    @GetMapping("/jobs/recent")
    public ResponseEntity<ApiResponse<List<JobSummaryDto>>> getRecentJobs() {
        User currentUser = securityUtils.getCurrentUser().orElse(null);
        return ResponseEntity.ok(ApiResponse.success("Recent jobs fetched", jobService.getRecentJobs(currentUser)));
    }

    @GetMapping("/jobs/categories")
    public ResponseEntity<ApiResponse<List<String>>> getCategories() {
        return ResponseEntity.ok(ApiResponse.success("Categories fetched", jobService.getCategories()));
    }

    @PostMapping("/jobs")
    @PreAuthorize("hasAuthority('ROLE_RECRUITER')")
    public ResponseEntity<ApiResponse<JobDetailDto>> createJob(@Valid @RequestBody JobCreateUpdateRequest request) {
        User user = securityUtils.getCurrentUserOrThrow();
        Recruiter recruiter = userService.getRecruiter(user);
        JobDetailDto created = jobService.createJob(request, recruiter);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Job posted successfully", created));
    }

    @PutMapping("/jobs/{id}")
    @PreAuthorize("hasAuthority('ROLE_RECRUITER')")
    public ResponseEntity<ApiResponse<JobDetailDto>> updateJob(@PathVariable Long id,
                                                               @Valid @RequestBody JobCreateUpdateRequest request) {
        User user = securityUtils.getCurrentUserOrThrow();
        Recruiter recruiter = userService.getRecruiter(user);
        JobDetailDto updated = jobService.updateJob(id, request, recruiter);
        return ResponseEntity.ok(ApiResponse.success("Job updated successfully", updated));
    }

    @DeleteMapping("/jobs/{id}")
    @PreAuthorize("hasAuthority('ROLE_RECRUITER')")
    public ResponseEntity<ApiResponse<Void>> deleteJob(@PathVariable Long id) {
        User user = securityUtils.getCurrentUserOrThrow();
        Recruiter recruiter = userService.getRecruiter(user);
        jobService.deleteJob(id, recruiter);
        return ResponseEntity.ok(ApiResponse.success("Job deleted successfully"));
    }

    @GetMapping("/companies")
    public ResponseEntity<ApiResponse<List<CompanyDto>>> getCompanies() {
        return ResponseEntity.ok(ApiResponse.success("Companies fetched", companyService.getAllCompanies()));
    }

    @GetMapping("/companies/{id}")
    public ResponseEntity<ApiResponse<CompanyDto>> getCompany(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Company fetched", companyService.getCompanyById(id)));
    }
}
