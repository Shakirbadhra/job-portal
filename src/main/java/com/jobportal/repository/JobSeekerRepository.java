package com.jobportal.repository;

import com.jobportal.entity.JobSeeker;
import com.jobportal.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JobSeekerRepository extends JpaRepository<JobSeeker, Long> {
    Optional<JobSeeker> findByUser(User user);
    Optional<JobSeeker> findByUserId(Long userId);
}
