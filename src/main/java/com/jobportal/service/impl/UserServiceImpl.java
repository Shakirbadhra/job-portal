package com.jobportal.service.impl;

import com.jobportal.dto.request.ProfileUpdateRequest;
import com.jobportal.dto.request.RegisterRequest;
import com.jobportal.dto.response.UserProfileDto;
import com.jobportal.entity.*;
import com.jobportal.entity.enums.RoleName;
import com.jobportal.exception.BadRequestException;
import com.jobportal.exception.DuplicateResourceException;
import com.jobportal.exception.ResourceNotFoundException;
import com.jobportal.repository.*;
import com.jobportal.service.CompanyService;
import com.jobportal.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JobSeekerRepository jobSeekerRepository;
    private final RecruiterRepository recruiterRepository;
    private final SkillRepository skillRepository;
    private final CompanyService companyService;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository,
                           RoleRepository roleRepository,
                           JobSeekerRepository jobSeekerRepository,
                           RecruiterRepository recruiterRepository,
                           SkillRepository skillRepository,
                           CompanyService companyService,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.jobSeekerRepository = jobSeekerRepository;
        this.recruiterRepository = recruiterRepository;
        this.skillRepository = skillRepository;
        this.companyService = companyService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("An account with email " + email + " already exists");
        }

        RoleName roleName;
        String requestedRole = request.getRole().trim().toUpperCase();
        if ("RECRUITER".equals(requestedRole) || "ROLE_RECRUITER".equals(requestedRole)) {
            roleName = RoleName.ROLE_RECRUITER;
        } else if ("JOB_SEEKER".equals(requestedRole) || "ROLE_JOB_SEEKER".equals(requestedRole)) {
            roleName = RoleName.ROLE_JOB_SEEKER;
        } else {
            throw new BadRequestException("Invalid role specified. Must be JOB_SEEKER or RECRUITER");
        }

        Role role = roleRepository.findByName(roleName)
                .orElseGet(() -> roleRepository.save(new Role(roleName)));

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName().trim());
        user.setPhone(request.getPhone() != null ? request.getPhone().trim() : null);
        user.setRole(role);
        user.setEnabled(true);
        user = userRepository.save(user);

        if (roleName == RoleName.ROLE_JOB_SEEKER) {
            JobSeeker seeker = new JobSeeker(user);
            jobSeekerRepository.save(seeker);
            user.setJobSeeker(seeker);
        } else if (roleName == RoleName.ROLE_RECRUITER) {
            Company company = null;
            if (StringUtils.hasText(request.getCompanyName())) {
                company = companyService.getOrCreateCompany(
                        request.getCompanyName(),
                        request.getCompanyWebsite(),
                        request.getCompanyIndustry()
                );
            }
            Recruiter recruiter = new Recruiter(user, company, request.getDesignation());
            recruiterRepository.save(recruiter);
            user.setRecruiter(recruiter);
        }

        return user;
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileDto getUserProfile(User user) {
        UserProfileDto dto = new UserProfileDto();
        dto.setUserId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setFullName(user.getFullName());
        dto.setPhone(user.getPhone());
        dto.setRole(user.getRole().getName().name());
        dto.setEnabled(user.isEnabled());
        dto.setCreatedAt(user.getCreatedAt());

        if (user.getRole().getName() == RoleName.ROLE_JOB_SEEKER) {
            jobSeekerRepository.findByUserId(user.getId()).ifPresent(seeker -> {
                dto.setSeekerId(seeker.getId());
                dto.setHeadline(seeker.getHeadline());
                dto.setBio(seeker.getBio());
                dto.setCurrentLocation(seeker.getCurrentLocation());
                dto.setTotalExperienceYears(seeker.getTotalExperienceYears());
                dto.setHighestQualification(seeker.getHighestQualification());
                dto.setProfileImageUrl(seeker.getProfileImageUrl());
                dto.setSkills(seeker.getSkills().stream().map(Skill::getName).collect(Collectors.toSet()));

                seeker.getResumes().stream()
                        .filter(Resume::isPrimary)
                        .findFirst()
                        .ifPresent(r -> {
                            dto.setPrimaryResumeId(r.getId());
                            dto.setPrimaryResumeFilename(r.getOriginalFilename());
                        });
            });
        } else if (user.getRole().getName() == RoleName.ROLE_RECRUITER) {
            recruiterRepository.findByUserId(user.getId()).ifPresent(recruiter -> {
                dto.setRecruiterId(recruiter.getId());
                dto.setDesignation(recruiter.getDesignation());
                dto.setOfficePhone(recruiter.getOfficePhone());
                if (recruiter.getCompany() != null) {
                    dto.setCompanyId(recruiter.getCompany().getId());
                    dto.setCompanyName(recruiter.getCompany().getName());
                    dto.setCompanyLogoUrl(recruiter.getCompany().getLogoUrl());
                }
            });
        }

        return dto;
    }

    @Override
    public UserProfileDto updateProfile(User user, ProfileUpdateRequest request) {
        if (StringUtils.hasText(request.getFullName())) {
            user.setFullName(request.getFullName().trim());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone().trim());
        }
        final User savedUser = userRepository.save(user);

        if (savedUser.getRole().getName() == RoleName.ROLE_JOB_SEEKER) {
            JobSeeker seeker = jobSeekerRepository.findByUserId(savedUser.getId())
                    .orElseGet(() -> jobSeekerRepository.save(new JobSeeker(savedUser)));

            if (request.getHeadline() != null) seeker.setHeadline(request.getHeadline().trim());
            if (request.getBio() != null) seeker.setBio(request.getBio().trim());
            if (request.getCurrentLocation() != null) seeker.setCurrentLocation(request.getCurrentLocation().trim());
            if (request.getTotalExperienceYears() != null) seeker.setTotalExperienceYears(request.getTotalExperienceYears());
            if (request.getHighestQualification() != null) seeker.setHighestQualification(request.getHighestQualification().trim());

            if (request.getSkills() != null) {
                Set<Skill> skillEntities = new HashSet<>();
                for (String skillName : request.getSkills()) {
                    if (StringUtils.hasText(skillName)) {
                        String clean = skillName.trim();
                        Skill skill = skillRepository.findByNameIgnoreCase(clean)
                                .orElseGet(() -> skillRepository.save(new Skill(clean)));
                        skillEntities.add(skill);
                    }
                }
                seeker.setSkills(skillEntities);
            }
            jobSeekerRepository.save(seeker);
        } else if (savedUser.getRole().getName() == RoleName.ROLE_RECRUITER) {
            Recruiter recruiter = recruiterRepository.findByUserId(savedUser.getId())
                    .orElseGet(() -> recruiterRepository.save(new Recruiter(savedUser, null, null)));

            if (request.getDesignation() != null) recruiter.setDesignation(request.getDesignation().trim());
            if (request.getOfficePhone() != null) recruiter.setOfficePhone(request.getOfficePhone().trim());
            recruiterRepository.save(recruiter);
        }

        return getUserProfile(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserProfileDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::getUserProfile)
                .collect(Collectors.toList());
    }

    @Override
    public boolean toggleUserStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        user.setEnabled(!user.isEnabled());
        userRepository.save(user);
        return user.isEnabled();
    }

    @Override
    @Transactional(readOnly = true)
    public JobSeeker getJobSeeker(User user) {
        return jobSeekerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Job seeker profile not found for user: " + user.getEmail()));
    }

    @Override
    @Transactional(readOnly = true)
    public Recruiter getRecruiter(User user) {
        return recruiterRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Recruiter profile not found for user: " + user.getEmail()));
    }

    @Override
    public void changePassword(User user, String currentPassword, String newPassword) {
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BadRequestException("Your current password is incorrect");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new BadRequestException("New password must be at least 6 characters");
        }
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new BadRequestException("New password must be different from the current password");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
