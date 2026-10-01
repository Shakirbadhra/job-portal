package com.jobportal.service.impl;

import com.jobportal.dto.response.ResumeResponseDto;
import com.jobportal.entity.JobSeeker;
import com.jobportal.entity.Resume;
import com.jobportal.entity.User;
import com.jobportal.repository.ApplicationRepository;
import com.jobportal.repository.RecruiterRepository;
import com.jobportal.exception.BadRequestException;
import com.jobportal.exception.FileStorageException;
import com.jobportal.exception.ResourceNotFoundException;
import com.jobportal.repository.ResumeRepository;
import com.jobportal.service.ResumeService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class ResumeServiceImpl implements ResumeService {

    @Value("${jobportal.upload.resume-dir:uploads/resumes}")
    private String uploadDir;

    private Path fileStorageLocation;
    private final ResumeRepository resumeRepository;
    private final ApplicationRepository applicationRepository;
    private final RecruiterRepository recruiterRepository;

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(".pdf", ".doc", ".docx");

    public ResumeServiceImpl(ResumeRepository resumeRepository,
                             ApplicationRepository applicationRepository,
                             RecruiterRepository recruiterRepository) {
        this.applicationRepository = applicationRepository;
        this.recruiterRepository = recruiterRepository;
        this.resumeRepository = resumeRepository;
    }

    @PostConstruct
    public void init() {
        this.fileStorageLocation = Paths.get(this.uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new FileStorageException("Could not create resume upload directory at: " + this.fileStorageLocation, ex);
        }
    }

    @Override
    public ResumeResponseDto uploadResume(MultipartFile file, JobSeeker seeker) {
        if (file.isEmpty()) {
            throw new BadRequestException("Uploaded file is empty");
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        if (originalFilename.contains("..")) {
            throw new BadRequestException("Invalid filename sequence: " + originalFilename);
        }

        String lowerName = originalFilename.toLowerCase();
        boolean validExt = ALLOWED_EXTENSIONS.stream().anyMatch(lowerName::endsWith);
        if (!validExt) {
            throw new BadRequestException("Invalid file type. Supported formats are: PDF, DOC, DOCX");
        }

        String extension = "";
        int dotIdx = originalFilename.lastIndexOf('.');
        if (dotIdx > 0) {
            extension = originalFilename.substring(dotIdx);
        }

        String storedFilename = UUID.randomUUID().toString() + extension;

        try {
            Path targetLocation = this.fileStorageLocation.resolve(storedFilename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            boolean isFirst = resumeRepository.findByJobSeekerOrderByUploadedAtDesc(seeker).isEmpty();

            Resume resume = new Resume();
            resume.setJobSeeker(seeker);
            resume.setOriginalFilename(originalFilename);
            resume.setStoredFilename(storedFilename);
            resume.setFilePath(targetLocation.toString());
            resume.setFileSize(file.getSize());
            resume.setContentType(file.getContentType());
            resume.setPrimary(isFirst);

            resume = resumeRepository.save(resume);
            return mapToDto(resume);
        } catch (IOException ex) {
            throw new FileStorageException("Failed to store resume " + originalFilename + ". Please try again.", ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResumeResponseDto> getSeekerResumes(JobSeeker seeker) {
        return resumeRepository.findByJobSeekerOrderByUploadedAtDesc(seeker).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public void setPrimaryResume(Long resumeId, JobSeeker seeker) {
        List<Resume> resumes = resumeRepository.findByJobSeekerOrderByUploadedAtDesc(seeker);
        for (Resume r : resumes) {
            r.setPrimary(r.getId().equals(resumeId));
            resumeRepository.save(r);
        }
    }

    @Override
    public void deleteResume(Long resumeId, JobSeeker seeker) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found with id: " + resumeId));

        if (!resume.getJobSeeker().getId().equals(seeker.getId())) {
            throw new BadRequestException("You do not own this resume");
        }

        try {
            Path path = Paths.get(resume.getFilePath());
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            // Log but don't prevent DB deletion
        }

        resumeRepository.delete(resume);

        // If deleted was primary, set new primary if exists
        List<Resume> remaining = resumeRepository.findByJobSeekerOrderByUploadedAtDesc(seeker);
        if (!remaining.isEmpty() && remaining.stream().noneMatch(Resume::isPrimary)) {
            remaining.get(0).setPrimary(true);
            resumeRepository.save(remaining.get(0));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Resource loadResumeResource(Long resumeId) {
        Resume resume = getResumeEntity(resumeId);
        try {
            Path filePath = this.fileStorageLocation.resolve(resume.getStoredFilename()).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("Resume file not found on disk");
            }
        } catch (MalformedURLException ex) {
            throw new ResourceNotFoundException("Resume file path error: " + ex.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Resume getResumeEntity(Long resumeId) {
        return resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found with id: " + resumeId));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canAccessResume(Long resumeId, User user) {
        Resume resume = getResumeEntity(resumeId);
        switch (user.getRole().getName()) {
            case ROLE_ADMIN:
                return true;
            case ROLE_JOB_SEEKER:
                return resume.getJobSeeker().getUser().getId().equals(user.getId());
            case ROLE_RECRUITER:
                return recruiterRepository.findByUserId(user.getId())
                        .map(r -> applicationRepository.existsByResume_IdAndJob_Recruiter_Id(resumeId, r.getId()))
                        .orElse(false);
            default:
                return false;
        }
    }

    private ResumeResponseDto mapToDto(Resume r) {
        ResumeResponseDto dto = new ResumeResponseDto();
        dto.setId(r.getId());
        dto.setOriginalFilename(r.getOriginalFilename());
        dto.setStoredFilename(r.getStoredFilename());
        dto.setFileSize(r.getFileSize());
        dto.setContentType(r.getContentType());
        dto.setPrimary(r.isPrimary());
        dto.setUploadedAt(r.getUploadedAt());
        dto.setDownloadUrl("/api/resumes/download/" + r.getId());
        return dto;
    }
}
