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

                predicates.add(cb.or(titleMatch, descMatch, compMatch, skillExists(root, query, cb, term, true)));
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

            // Specific Skill (sub-query keeps one row per job, so no DISTINCT is needed)
            if (StringUtils.hasText(criteria.getSkill())) {
                predicates.add(skillExists(root, query, cb, criteria.getSkill().trim().toLowerCase(), false));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static Predicate skillExists(Root<Job> root, CriteriaQuery<?> query, CriteriaBuilder cb, String value, boolean like) {
        Subquery<Long> sub = query.subquery(Long.class);
        Root<Job> subJob = sub.from(Job.class);
        Join<Job, Skill> skill = subJob.join("skills");
        Predicate match = like ? cb.like(cb.lower(skill.get("name")), value) : cb.equal(cb.lower(skill.get("name")), value);
        sub.select(subJob.get("id")).where(cb.equal(subJob.get("id"), root.get("id")), match);
        return cb.exists(sub);
    }
}
