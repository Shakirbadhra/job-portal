package com.jobportal.service.impl;

import com.jobportal.dto.request.CompanyUpdateRequest;
import com.jobportal.dto.response.CompanyDto;
import com.jobportal.entity.Company;
import com.jobportal.entity.enums.JobStatus;
import com.jobportal.exception.ResourceNotFoundException;
import com.jobportal.repository.CompanyRepository;
import com.jobportal.repository.JobRepository;
import com.jobportal.service.CompanyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;
    private final JobRepository jobRepository;

    public CompanyServiceImpl(CompanyRepository companyRepository, JobRepository jobRepository) {
        this.companyRepository = companyRepository;
        this.jobRepository = jobRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompanyDto> getAllCompanies() {
        return companyRepository.findAllOrderByName().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyDto getCompanyById(Long id) {
        Company company = getCompanyEntity(id);
        return mapToDto(company);
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyDto getCompanyBySlug(String slug) {
        Company company = companyRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with slug: " + slug));
        return mapToDto(company);
    }

    @Override
    public CompanyDto updateCompany(Long id, CompanyUpdateRequest request) {
        Company company = getCompanyEntity(id);

        if (StringUtils.hasText(request.getName())) {
            company.setName(request.getName().trim());
        }
        if (request.getLogoUrl() != null) company.setLogoUrl(request.getLogoUrl().trim());
        if (request.getWebsite() != null) company.setWebsite(request.getWebsite().trim());
        if (request.getIndustry() != null) company.setIndustry(request.getIndustry().trim());
        if (request.getDescription() != null) company.setDescription(request.getDescription().trim());
        if (request.getLocation() != null) company.setLocation(request.getLocation().trim());
        if (request.getCompanySize() != null) company.setCompanySize(request.getCompanySize().trim());
        if (request.getFoundedYear() != null) company.setFoundedYear(request.getFoundedYear());

        company = companyRepository.save(company);
        return mapToDto(company);
    }

    @Override
    @Transactional(readOnly = true)
    public Company getCompanyEntity(Long id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with id: " + id));
    }

    @Override
    public Company getOrCreateCompany(String name, String website, String industry) {
        String cleanName = name.trim();
        return companyRepository.findByName(cleanName)
                .orElseGet(() -> {
                    Company c = new Company();
                    c.setName(cleanName);
                    c.setWebsite(website != null ? website.trim() : null);
                    c.setIndustry(industry != null ? industry.trim() : "Technology");
                    c.setLocation("Global");
                    return companyRepository.save(c);
                });
    }

    private CompanyDto mapToDto(Company c) {
        CompanyDto dto = new CompanyDto();
        dto.setId(c.getId());
        dto.setName(c.getName());
        dto.setSlug(c.getSlug());
        dto.setLogoUrl(c.getLogoUrl());
        dto.setWebsite(c.getWebsite());
        dto.setIndustry(c.getIndustry());
        dto.setDescription(c.getDescription());
        dto.setLocation(c.getLocation());
        dto.setCompanySize(c.getCompanySize());
        dto.setFoundedYear(c.getFoundedYear());

        long activeCount = c.getJobs().stream()
                .filter(j -> j.getStatus() == JobStatus.APPROVED)
                .count();
        dto.setOpenJobsCount(activeCount);
        return dto;
    }
}
