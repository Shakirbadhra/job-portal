package com.jobportal.service;

import com.jobportal.dto.response.ResumeResponseDto;
import com.jobportal.entity.JobSeeker;
import com.jobportal.entity.Resume;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ResumeService {
    ResumeResponseDto uploadResume(MultipartFile file, JobSeeker seeker);
    List<ResumeResponseDto> getSeekerResumes(JobSeeker seeker);
    void setPrimaryResume(Long resumeId, JobSeeker seeker);
    void deleteResume(Long resumeId, JobSeeker seeker);
    Resource loadResumeResource(Long resumeId);
    Resume getResumeEntity(Long resumeId);

    /** A resume is visible to its owner, to admins, and to recruiters who received an application with it. */
    boolean canAccessResume(Long resumeId, com.jobportal.entity.User user);
}
