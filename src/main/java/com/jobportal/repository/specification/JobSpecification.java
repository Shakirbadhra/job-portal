package com.jobportal.repository.specification;

import com.jobportal.dto.request.JobSearchCriteria;
import com.jobportal.entity.Company;
import com.jobportal.entity.Job;
import com.jobportal.entity.Skill;
import com.jobportal.entity.enums.JobStatus;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;

public class JobSpecification {

    public static Specification<Job> build(JobSearchCriteria criteria) {
        return (Root<Job> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always enforce status filter (defaults to APPROVED for public searches)
            if (criteria.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), criteria.getStatus()));
            } else {
                predicates.add(cb.equal(root.get("status"), JobStatus.APPROVED));
            }

            // Keyword: search in title, description, company name
            if (StringUtils.hasText(criteria.getKeyword())) {
                String term = "%" + criteria.getKeyword().trim().toLowerCase() + "%";
                Join<Job, Company> companyJoin = root.join("company", JoinType.LEFT);

                Predicate titleMatch = cb.like(cb.lower(root.get("title")), term);
                Predicate descMatch = cb.like(cb.lower(root.get("description")), term);
                Predicate compMatch = cb.like(cb.lower(companyJoin.get("name")), term);

                predicates.add(cb.or(titleMatch, descMatch, compMatch));
            }

            // Location
            if (StringUtils.hasText(criteria.getLocation())) {
                String loc = "%" + criteria.getLocation().trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("location")), loc));
            }

            // Category
            if (StringUtils.hasText(criteria.getCategory())) {
                predicates.add(cb.equal(cb.lower(root.get("jobCategory")), criteria.getCategory().trim().toLowerCase()));
            }

            // Employment Type
            if (criteria.getEmploymentType() != null) {
                predicates.add(cb.equal(root.get("employmentType"), criteria.getEmploymentType()));
            }

            // Workplace Type
            if (criteria.getWorkplaceType() != null) {
                predicates.add(cb.equal(root.get("workplaceType"), criteria.getWorkplaceType()));
            }

            // Experience Level
            if (criteria.getExperienceLevel() != null) {
                predicates.add(cb.equal(root.get("experienceLevel"), criteria.getExperienceLevel()));
            }

            // Min Experience Years
            if (criteria.getMinExperience() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("minExperienceYears"), criteria.getMinExperience()));
            }

            // Salary Range
            if (criteria.getMinSalary() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("maxSalary"), criteria.getMinSalary()));
            }
            if (criteria.getMaxSalary() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("minSalary"), criteria.getMaxSalary()));
            }

            // Company ID
            if (criteria.getCompanyId() != null) {
                predicates.add(cb.equal(root.get("company").get("id"), criteria.getCompanyId()));
            }

            // Specific Skill
            if (StringUtils.hasText(criteria.getSkill())) {
                Join<Job, Skill> skillJoin = root.join("skills", JoinType.LEFT);
                predicates.add(cb.equal(cb.lower(skillJoin.get("name")), criteria.getSkill().trim().toLowerCase()));
            }

            // Distinct results if joins are present
            query.distinct(true);

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
