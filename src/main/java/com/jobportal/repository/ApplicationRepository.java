package com.jobportal.repository;

import com.jobportal.entity.Application;
import com.jobportal.entity.Job;
import com.jobportal.entity.JobSeeker;
import com.jobportal.entity.enums.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    boolean existsByJobAndJobSeeker(Job job, JobSeeker jobSeeker);

    Optional<Application> findByJobAndJobSeeker(Job job, JobSeeker jobSeeker);

    List<Application> findByJobSeekerOrderByAppliedAtDesc(JobSeeker jobSeeker);

    Page<Application> findByJobSeekerOrderByAppliedAtDesc(JobSeeker jobSeeker, Pageable pageable);

    List<Application> findByJobOrderByAppliedAtDesc(Job job);

    Page<Application> findByJobOrderByAppliedAtDesc(Job job, Pageable pageable);

    @Query("SELECT a FROM Application a WHERE a.job.recruiter.id = :recruiterId ORDER BY a.appliedAt DESC")
    Page<Application> findByRecruiterId(@Param("recruiterId") Long recruiterId, Pageable pageable);

    @Query("SELECT a FROM Application a WHERE a.job.recruiter.id = :recruiterId ORDER BY a.appliedAt DESC")
    List<Application> findAllByRecruiterId(@Param("recruiterId") Long recruiterId);

    long countByJobSeeker(JobSeeker jobSeeker);

    long countByJobSeekerAndStatus(JobSeeker jobSeeker, ApplicationStatus status);

    @Query("SELECT COUNT(a) FROM Application a WHERE a.job.recruiter.id = :recruiterId")
    long countByRecruiterId(@Param("recruiterId") Long recruiterId);

    @Query("SELECT COUNT(a) FROM Application a WHERE a.job.recruiter.id = :recruiterId AND a.status = :status")
    long countByRecruiterIdAndStatus(@Param("recruiterId") Long recruiterId, @Param("status") ApplicationStatus status);

    long countByStatus(ApplicationStatus status);

    @Query("SELECT a.status, COUNT(a) FROM Application a GROUP BY a.status")
    List<Object[]> countApplicationsByStatus();
}
