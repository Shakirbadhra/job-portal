package com.jobportal.service;

import com.jobportal.dto.request.CompanyUpdateRequest;
import com.jobportal.dto.response.CompanyDto;
import com.jobportal.entity.Company;

import java.util.List;

public interface CompanyService {
    List<CompanyDto> getAllCompanies();
    CompanyDto getCompanyById(Long id);
    CompanyDto getCompanyBySlug(String slug);
    CompanyDto updateCompany(Long id, CompanyUpdateRequest request);
    Company getCompanyEntity(Long id);
    Company getOrCreateCompany(String name, String website, String industry);
}
