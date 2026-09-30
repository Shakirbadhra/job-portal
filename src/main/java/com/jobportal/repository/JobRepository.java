package com.jobportal.repository;

import com.jobportal.entity.Company;
import com.jobportal.entity.Job;
import com.jobportal.entity.Recruiter;
import com.jobportal.entity.enums.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobRepository extends JpaRepository<Job, Long>, JpaSpecificationExecutor<Job> {

    Optional<Job> findBySlug(String slug);

    Page<Job> findByStatus(JobStatus status, Pageable pageable);

    List<Job> findTop6ByStatusAndIsFeaturedTrueOrderByCreatedAtDesc(JobStatus status);

    List<Job> findTop8ByStatusOrderByCreatedAtDesc(JobStatus status);

    Page<Job> findByRecruiter(Recruiter recruiter, Pageable pageable);

    List<Job> findByRecruiterOrderByCreatedAtDesc(Recruiter recruiter);

    Page<Job> findByCompanyAndStatus(Company company, JobStatus status, Pageable pageable);

    long countByStatus(JobStatus status);

    long countByRecruiter(Recruiter recruiter);

    long countByRecruiterAndStatus(Recruiter recruiter, JobStatus status);

    @Query("SELECT DISTINCT j.jobCategory FROM Job j WHERE j.status = :status ORDER BY j.jobCategory ASC")
    List<String> findDistinctJobCategories(@Param("status") JobStatus status);

    @Query("SELECT j.jobCategory, COUNT(j) FROM Job j WHERE j.status = :status GROUP BY j.jobCategory ORDER BY COUNT(j) DESC")
    List<Object[]> countJobsByCategory(@Param("status") JobStatus status);

    @Query("SELECT c.name, COUNT(j) FROM Job j JOIN j.company c WHERE j.status = :status GROUP BY c.id, c.name ORDER BY COUNT(j) DESC")
    List<Object[]> countJobsByCompany(@Param("status") JobStatus status);
}
