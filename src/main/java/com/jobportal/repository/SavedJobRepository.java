package com.jobportal.repository;

import com.jobportal.entity.Job;
import com.jobportal.entity.JobSeeker;
import com.jobportal.entity.SavedJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavedJobRepository extends JpaRepository<SavedJob, Long> {
    boolean existsByJobSeekerAndJob(JobSeeker jobSeeker, Job job);
    Optional<SavedJob> findByJobSeekerAndJob(JobSeeker jobSeeker, Job job);
    List<SavedJob> findByJobSeekerOrderBySavedAtDesc(JobSeeker jobSeeker);
    Page<SavedJob> findByJobSeekerOrderBySavedAtDesc(JobSeeker jobSeeker, Pageable pageable);
    long countByJobSeeker(JobSeeker jobSeeker);
    void deleteByJobSeekerAndJob(JobSeeker jobSeeker, Job job);
}
