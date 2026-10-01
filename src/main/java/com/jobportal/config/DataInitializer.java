package com.jobportal.config;

import com.jobportal.entity.*;
import com.jobportal.entity.enums.*;
import com.jobportal.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final JobSeekerRepository jobSeekerRepository;
    private final RecruiterRepository recruiterRepository;
    private final SkillRepository skillRepository;
    private final JobRepository jobRepository;
    private final ResumeRepository resumeRepository;
    private final ApplicationRepository applicationRepository;
    private final SavedJobRepository savedJobRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${jobportal.upload.resume-dir:uploads/resumes}")
    private String resumeDir;

    public DataInitializer(RoleRepository roleRepository,
                           UserRepository userRepository,
                           CompanyRepository companyRepository,
                           JobSeekerRepository jobSeekerRepository,
                           RecruiterRepository recruiterRepository,
                           SkillRepository skillRepository,
                           JobRepository jobRepository,
                           ResumeRepository resumeRepository,
                           ApplicationRepository applicationRepository,
                           SavedJobRepository savedJobRepository,
                           PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.jobSeekerRepository = jobSeekerRepository;
        this.recruiterRepository = recruiterRepository;
        this.skillRepository = skillRepository;
        this.jobRepository = jobRepository;
        this.resumeRepository = resumeRepository;
        this.applicationRepository = applicationRepository;
        this.savedJobRepository = savedJobRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (roleRepository.count() > 0 && jobRepository.count() >= 30) {
            log.info("Database already seeded. Skipping initial data population.");
            return;
        }

        log.info("Starting database seeding with realistic enterprise data...");

        // 1. Roles
        Role seekerRole = roleRepository.findByName(RoleName.ROLE_JOB_SEEKER)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.ROLE_JOB_SEEKER)));
        Role recruiterRole = roleRepository.findByName(RoleName.ROLE_RECRUITER)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.ROLE_RECRUITER)));
        Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.ROLE_ADMIN)));

        // 2. Skills
        Map<String, Skill> skillsMap = new HashMap<>();
        String[] skillNames = {
                "Java", "Spring Boot", "Hibernate", "Microservices", "REST APIs",
                "MySQL", "PostgreSQL", "MongoDB", "Redis",
                "Python", "FastAPI", "Django", "Machine Learning", "PyTorch", "TensorFlow",
                "React", "TypeScript", "JavaScript", "Next.js", "Vue.js", "HTML5", "CSS3", "Bootstrap",
                "Docker", "Kubernetes", "AWS", "Google Cloud", "Azure", "Terraform", "CI/CD",
                "Figma", "UI/UX Design", "Wireframing", "User Research",
                "Product Strategy", "Agile", "Scrum", "JIRA", "Data Analytics"
        };
        for (String name : skillNames) {
            Skill skill = skillRepository.findByNameIgnoreCase(name)
                    .orElseGet(() -> skillRepository.save(new Skill(name)));
            skillsMap.put(name, skill);
        }

        // 3. Admin User
        if (!userRepository.existsByEmail("admin@jobconnect.com")) {
            User admin = new User("admin@jobconnect.com", passwordEncoder.encode("Admin@123"), "System Administrator", adminRole);
            admin.setPhone("+1 (555) 010-0001");
            userRepository.save(admin);
            log.info("Created Admin user: admin@jobconnect.com / Admin@123");
        }

        // 4. Companies (12 realistic enterprise & high-growth companies)
        Map<String, Company> companies = new HashMap<>();

        Object[][] companyData = {
                {"Google", "https://logo.clearbit.com/google.com", "https://google.com/careers", "Technology", "Global tech leader organizing the world's information and making it universally accessible.", "Mountain View, CA", "1000+", 1998},
                {"Microsoft", "https://logo.clearbit.com/microsoft.com", "https://careers.microsoft.com", "Cloud & Enterprise Software", "Empowering every person and organization on the planet to achieve more through cloud computing and productivity tools.", "Redmond, WA", "1000+", 1975},
                {"Amazon", "https://logo.clearbit.com/amazon.com", "https://amazon.jobs", "E-Commerce & Cloud Computing", "Earth's most customer-centric company, pioneering AWS cloud infrastructure, logistics, and digital streaming.", "Seattle, WA", "1000+", 1994},
                {"Spotify", "https://logo.clearbit.com/spotify.com", "https://www.lifeatspotify.com", "Media & Audio Streaming", "Transforming music and podcast listening forever with innovative personalization and streaming audio tech.", "New York, NY", "1000+", 2006},
                {"Netflix", "https://logo.clearbit.com/netflix.com", "https://jobs.netflix.com", "Entertainment & Streaming", "Leading entertainment service with hundreds of millions of paid memberships across 190+ countries.", "Los Gatos, CA", "1000+", 1997},
                {"Adobe", "https://logo.clearbit.com/adobe.com", "https://careers.adobe.com", "Creative & Cloud Software", "Changing the world through digital experiences, empowering creative professionals, businesses, and developers.", "San Jose, CA", "1000+", 1982},
                {"Stripe", "https://logo.clearbit.com/stripe.com", "https://stripe.com/jobs", "Fintech & Payments", "Financial infrastructure for the internet, powering transactions for billions of people and businesses worldwide.", "San Francisco, CA", "1000+", 2010},
                {"Salesforce", "https://logo.clearbit.com/salesforce.com", "https://salesforce.com/careers", "Enterprise Cloud & CRM", "World's #1 customer relationship management platform connecting businesses with customers through AI and cloud.", "San Francisco, CA", "1000+", 1999},
                {"Meta", "https://logo.clearbit.com/meta.com", "https://metacareers.com", "Social Media & VR", "Building technologies that help people connect, find communities, and grow businesses across apps and the metaverse.", "Menlo Park, CA", "1000+", 2004},
                {"Uber", "https://logo.clearbit.com/uber.com", "https://uber.com/careers", "Mobility & Logistics", "Reimagining the way the world moves for the better through ride-hailing, food delivery, and freight technology.", "San Francisco, CA", "1000+", 2009},
                {"Airbnb", "https://logo.clearbit.com/airbnb.com", "https://careers.airbnb.com", "Travel & Hospitality Tech", "Global travel community offering one-of-a-kind stays and experiences hosted by locals around the world.", "San Francisco, CA", "1000+", 2008},
                {"GitHub", "https://logo.clearbit.com/github.com", "https://github.com/about/careers", "Developer Tools", "The world's leading AI-powered developer platform where over 100 million developers build and collaborate.", "Remote / Global", "1000+", 2008}
        };

        for (Object[] c : companyData) {
            String name = (String) c[0];
            Company comp = companyRepository.findByName(name).orElseGet(() -> {
                Company entity = new Company();
                entity.setName(name);
                // Logos render as branded monograms in the UI; recruiters can set a logo URL from their company profile
                entity.setLogoUrl(null);
                entity.setWebsite((String) c[2]);
                entity.setIndustry((String) c[3]);
                entity.setDescription((String) c[4]);
                entity.setLocation((String) c[5]);
                entity.setCompanySize((String) c[6]);
                entity.setFoundedYear((Integer) c[7]);
                return companyRepository.save(entity);
            });
            companies.put(name, comp);
        }

        // 5. Recruiters
        List<Recruiter> recruitersList = new ArrayList<>();
        Object[][] recruiterUsers = {
                {"recruiter@google.com", "Sarah Jenkins", "Senior Talent Partner", "Google"},
                {"recruiter@microsoft.com", "David Chen", "Lead Technical Recruiter", "Microsoft"},
                {"recruiter@amazon.com", "Rachel Adams", "Principal Recruiter, AWS", "Amazon"},
                {"recruiter@spotify.com", "Liam Miller", "Head of Engineering Hiring", "Spotify"},
                {"recruiter@stripe.com", "Elena Rostova", "Staff Technical Recruiter", "Stripe"}
        };

        for (Object[] ru : recruiterUsers) {
            String email = (String) ru[0];
            String fullName = (String) ru[1];
            String designation = (String) ru[2];
            String companyName = (String) ru[3];

            User user = userRepository.findByEmail(email).orElseGet(() -> {
                User u = new User(email, passwordEncoder.encode("Recruiter@123"), fullName, recruiterRole);
                u.setPhone("+1 (555) 234-5678");
                return userRepository.save(u);
            });

            Company company = companies.get(companyName);
            Recruiter recruiter = recruiterRepository.findByUserId(user.getId()).orElseGet(() -> {
                Recruiter r = new Recruiter(user, company, designation);
                r.setOfficePhone("+1 (555) 800-" + (1000 + recruitersList.size()));
                return recruiterRepository.save(r);
            });
            recruitersList.add(recruiter);
        }

        // 6. Job Seekers
        List<JobSeeker> seekersList = new ArrayList<>();
        Object[][] seekerUsers = {
                {"seeker@example.com", "Alex Morgan", "Full Stack Java & Cloud Engineer", "Austin, TX", 4, "B.S. in Computer Science", "Java,Spring Boot,MySQL,Docker,AWS,React"},
                {"sarah.dev@example.com", "Sarah Mitchell", "Senior Backend Architect | Microservices Specialist", "San Francisco, CA", 7, "M.S. in Software Engineering", "Java,Spring Boot,Microservices,Kubernetes,AWS,PostgreSQL"},
                {"kevin.ai@example.com", "Kevin Patel", "Machine Learning & Data Engineer", "Seattle, WA", 3, "B.S. in Data Science", "Python,Machine Learning,PyTorch,FastAPI,Docker"},
                {"emma.ui@example.com", "Emma Wilson", "Product Designer & UI/UX Specialist", "New York, NY", 5, "B.A. in Digital Arts & Design", "Figma,UI/UX Design,Wireframing,User Research,HTML5,CSS3"}
        };

        for (Object[] su : seekerUsers) {
            String email = (String) su[0];
            String name = (String) su[1];
            String headline = (String) su[2];
            String loc = (String) su[3];
            int exp = (Integer) su[4];
            String qual = (String) su[5];
            String skillList = (String) su[6];

            User user = userRepository.findByEmail(email).orElseGet(() -> {
                User u = new User(email, passwordEncoder.encode("Seeker@123"), name, seekerRole);
                u.setPhone("+1 (555) 300-" + (2000 + seekersList.size()));
                return userRepository.save(u);
            });

            JobSeeker seeker = jobSeekerRepository.findByUserId(user.getId()).orElseGet(() -> {
                JobSeeker s = new JobSeeker(user);
                s.setHeadline(headline);
                s.setCurrentLocation(loc);
                s.setTotalExperienceYears(exp);
                s.setHighestQualification(qual);
                s.setBio("Passionate professional with proven track record of designing scalable applications, solving complex problems, and collaborating in fast-paced cross-functional teams.");

                Set<Skill> sSkills = new HashSet<>();
                for (String sk : skillList.split(",")) {
                    if (skillsMap.containsKey(sk)) {
                        sSkills.add(skillsMap.get(sk));
                    }
                }
                s.setSkills(sSkills);
                return jobSeekerRepository.save(s);
            });
            seekersList.add(seeker);

            // Add sample resume for seeker
            if (resumeRepository.findByJobSeekerOrderByUploadedAtDesc(seeker).isEmpty()) {
                String storedName = UUID.randomUUID().toString() + ".pdf";
                Path pdfPath = writeSampleResumePdf(storedName, name, headline, email);
                Resume sampleResume = new Resume(
                        seeker,
                        name.replace(" ", "_") + "_Resume_2026.pdf",
                        storedName,
                        pdfPath.toString(),
                        pdfPath.toFile().length(),
                        "application/pdf"
                );
                sampleResume.setPrimary(true);
                resumeRepository.save(sampleResume);
            }
        }

        // 7. Seed 32 Realistic Job Postings
        Object[][] jobsCatalog = {
                // Title, Company, Category, EmpType, Workplace, ExpLevel, MinExp, MaxExp, MinSal, MaxSal, Location, Featured, Skills
                {"Senior Backend Engineer (Spring Boot)", "Google", "Software Engineering", EmploymentType.FULL_TIME, WorkplaceType.HYBRID, ExperienceLevel.SENIOR, 5, 10, new BigDecimal("165000"), new BigDecimal("225000"), "Mountain View, CA", true, new String[]{"Java", "Spring Boot", "Microservices", "Docker", "Kubernetes"}},
                {"Lead Cloud Solutions Architect", "Amazon", "DevOps & Cloud", EmploymentType.FULL_TIME, WorkplaceType.REMOTE, ExperienceLevel.SENIOR, 7, 14, new BigDecimal("180000"), new BigDecimal("250000"), "Seattle, WA", true, new String[]{"AWS", "Terraform", "Docker", "Kubernetes", "CI/CD"}},
                {"Full Stack Java Developer", "Microsoft", "Software Engineering", EmploymentType.FULL_TIME, WorkplaceType.HYBRID, ExperienceLevel.MID_LEVEL, 3, 6, new BigDecimal("130000"), new BigDecimal("170000"), "Redmond, WA", true, new String[]{"Java", "Spring Boot", "React", "TypeScript", "MySQL"}},
                {"Staff Machine Learning Engineer", "Meta", "Data & AI", EmploymentType.FULL_TIME, WorkplaceType.ONSITE, ExperienceLevel.SENIOR, 6, 12, new BigDecimal("195000"), new BigDecimal("275000"), "Menlo Park, CA", true, new String[]{"Python", "Machine Learning", "PyTorch", "FastAPI"}},
                {"Principal Product Designer (UI/UX)", "Spotify", "UI/UX Design", EmploymentType.FULL_TIME, WorkplaceType.REMOTE, ExperienceLevel.SENIOR, 6, 10, new BigDecimal("150000"), new BigDecimal("210000"), "New York, NY", true, new String[]{"Figma", "UI/UX Design", "Wireframing", "User Research"}},
                {"DevOps & Infrastructure Engineer", "Stripe", "DevOps & Cloud", EmploymentType.FULL_TIME, WorkplaceType.REMOTE, ExperienceLevel.MID_LEVEL, 3, 7, new BigDecimal("145000"), new BigDecimal("195000"), "San Francisco, CA", true, new String[]{"Docker", "Kubernetes", "AWS", "Terraform", "CI/CD"}},
                {"Frontend Engineer (React / TypeScript)", "Netflix", "Software Engineering", EmploymentType.FULL_TIME, WorkplaceType.HYBRID, ExperienceLevel.MID_LEVEL, 3, 6, new BigDecimal("140000"), new BigDecimal("185000"), "Los Gatos, CA", false, new String[]{"React", "TypeScript", "Next.js", "CSS3"}},
                {"Junior Java Developer", "Salesforce", "Software Engineering", EmploymentType.FULL_TIME, WorkplaceType.ONSITE, ExperienceLevel.JUNIOR, 1, 3, new BigDecimal("85000"), new BigDecimal("115000"), "San Francisco, CA", false, new String[]{"Java", "Spring Boot", "REST APIs", "MySQL"}},
                {"Senior Product Manager - Platform", "Uber", "Product Management", EmploymentType.FULL_TIME, WorkplaceType.HYBRID, ExperienceLevel.SENIOR, 5, 9, new BigDecimal("160000"), new BigDecimal("215000"), "San Francisco, CA", false, new String[]{"Product Strategy", "Agile", "Data Analytics", "JIRA"}},
                {"Associate Software Engineer (Fresher)", "Google", "Software Engineering", EmploymentType.FULL_TIME, WorkplaceType.ONSITE, ExperienceLevel.FRESHER, 0, 1, new BigDecimal("95000"), new BigDecimal("120000"), "Austin, TX", false, new String[]{"Java", "Python", "MySQL", "HTML5"}},
                {"Site Reliability Engineer (SRE)", "GitHub", "DevOps & Cloud", EmploymentType.FULL_TIME, WorkplaceType.REMOTE, ExperienceLevel.EXPERIENCED, 4, 8, new BigDecimal("155000"), new BigDecimal("205000"), "Remote / Global", false, new String[]{"Kubernetes", "Docker", "CI/CD", "AWS", "Python"}},
                {"Data Engineer - Big Data Pipelines", "Spotify", "Data & AI", EmploymentType.FULL_TIME, WorkplaceType.HYBRID, ExperienceLevel.MID_LEVEL, 3, 6, new BigDecimal("135000"), new BigDecimal("175000"), "Boston, MA", false, new String[]{"Python", "PostgreSQL", "AWS", "Docker"}},
                {"Lead Mobile Developer (iOS & Android)", "Adobe", "Software Engineering", EmploymentType.FULL_TIME, WorkplaceType.HYBRID, ExperienceLevel.SENIOR, 6, 10, new BigDecimal("160000"), new BigDecimal("220000"), "San Jose, CA", false, new String[]{"React", "TypeScript", "REST APIs"}},
                {"Cybersecurity Operations Specialist", "Microsoft", "Cybersecurity", EmploymentType.FULL_TIME, WorkplaceType.REMOTE, ExperienceLevel.EXPERIENCED, 4, 8, new BigDecimal("140000"), new BigDecimal("190000"), "Washington, DC", false, new String[]{"Azure", "Python", "CI/CD"}},
                {"Software Engineering Intern (Summer 2026)", "Amazon", "Software Engineering", EmploymentType.INTERNSHIP, WorkplaceType.HYBRID, ExperienceLevel.FRESHER, 0, 1, new BigDecimal("45000"), new BigDecimal("65000"), "Seattle, WA", false, new String[]{"Java", "Python", "MySQL"}},
                {"UI/UX Design Intern", "Airbnb", "UI/UX Design", EmploymentType.INTERNSHIP, WorkplaceType.REMOTE, ExperienceLevel.FRESHER, 0, 1, new BigDecimal("40000"), new BigDecimal("55000"), "San Francisco, CA", false, new String[]{"Figma", "UI/UX Design", "Wireframing"}},
                {"Enterprise Account Executive", "Salesforce", "Sales & Business", EmploymentType.FULL_TIME, WorkplaceType.HYBRID, ExperienceLevel.EXPERIENCED, 4, 8, new BigDecimal("120000"), new BigDecimal("240000"), "Chicago, IL", false, new String[]{"Product Strategy", "Agile"}},
                {"Senior Database Administrator (MySQL)", "Uber", "DevOps & Cloud", EmploymentType.FULL_TIME, WorkplaceType.HYBRID, ExperienceLevel.SENIOR, 6, 11, new BigDecimal("150000"), new BigDecimal("195000"), "Dallas, TX", false, new String[]{"MySQL", "PostgreSQL", "Redis", "AWS"}},
                {"AI Research Scientist", "Meta", "Data & AI", EmploymentType.FULL_TIME, WorkplaceType.ONSITE, ExperienceLevel.SENIOR, 5, 10, new BigDecimal("210000"), new BigDecimal("320000"), "New York, NY", false, new String[]{"Python", "Machine Learning", "PyTorch", "TensorFlow"}},
                {"Quality Assurance Automation Engineer", "Netflix", "Software Engineering", EmploymentType.FULL_TIME, WorkplaceType.REMOTE, ExperienceLevel.MID_LEVEL, 3, 6, new BigDecimal("115000"), new BigDecimal("155000"), "Los Angeles, CA", false, new String[]{"Java", "Python", "CI/CD", "Docker"}},
                {"Growth Marketing Manager", "Spotify", "Marketing & Growth", EmploymentType.FULL_TIME, WorkplaceType.HYBRID, ExperienceLevel.EXPERIENCED, 4, 7, new BigDecimal("110000"), new BigDecimal("150000"), "New York, NY", false, new String[]{"Data Analytics", "Product Strategy"}},
                {"Staff Infrastructure Engineer", "Stripe", "DevOps & Cloud", EmploymentType.FULL_TIME, WorkplaceType.REMOTE, ExperienceLevel.SENIOR, 7, 12, new BigDecimal("190000"), new BigDecimal("260000"), "Seattle, WA", false, new String[]{"Kubernetes", "AWS", "Terraform", "Docker"}},
                {"Technical Program Manager", "Google", "Product Management", EmploymentType.FULL_TIME, WorkplaceType.HYBRID, ExperienceLevel.SENIOR, 5, 9, new BigDecimal("155000"), new BigDecimal("210000"), "Mountain View, CA", false, new String[]{"Agile", "Scrum", "JIRA", "Product Strategy"}},
                {"Backend Engineer (Python & Microservices)", "Airbnb", "Software Engineering", EmploymentType.FULL_TIME, WorkplaceType.REMOTE, ExperienceLevel.MID_LEVEL, 2, 5, new BigDecimal("125000"), new BigDecimal("165000"), "Denver, CO", false, new String[]{"Python", "FastAPI", "PostgreSQL", "Docker"}},
                {"React Native Engineer", "Uber", "Software Engineering", EmploymentType.FULL_TIME, WorkplaceType.HYBRID, ExperienceLevel.MID_LEVEL, 3, 6, new BigDecimal("130000"), new BigDecimal("175000"), "San Francisco, CA", false, new String[]{"React", "TypeScript", "JavaScript"}},
                {"Human Resources & Talent Acquisition Lead", "Adobe", "Human Resources", EmploymentType.FULL_TIME, WorkplaceType.HYBRID, ExperienceLevel.EXPERIENCED, 5, 8, new BigDecimal("105000"), new BigDecimal("145000"), "San Jose, CA", false, new String[]{"Agile", "Product Strategy"}},
                {"Cloud Security Architect", "Microsoft", "Cybersecurity", EmploymentType.FULL_TIME, WorkplaceType.REMOTE, ExperienceLevel.SENIOR, 7, 12, new BigDecimal("175000"), new BigDecimal("235000"), "Atlanta, GA", false, new String[]{"Azure", "AWS", "Kubernetes", "Terraform"}},
                {"Contract Full Stack Engineer (6 Months)", "GitHub", "Software Engineering", EmploymentType.CONTRACT, WorkplaceType.REMOTE, ExperienceLevel.EXPERIENCED, 4, 8, new BigDecimal("110000"), new BigDecimal("150000"), "Remote", false, new String[]{"TypeScript", "React", "Docker", "REST APIs"}},
                {"Part-Time UI/UX Design Consultant", "Spotify", "UI/UX Design", EmploymentType.PART_TIME, WorkplaceType.REMOTE, ExperienceLevel.EXPERIENCED, 4, 8, new BigDecimal("65000"), new BigDecimal("85000"), "Remote", false, new String[]{"Figma", "UI/UX Design", "Wireframing"}},
                {"Senior Data Analyst", "Amazon", "Data & AI", EmploymentType.FULL_TIME, WorkplaceType.HYBRID, ExperienceLevel.MID_LEVEL, 3, 6, new BigDecimal("115000"), new BigDecimal("155000"), "Seattle, WA", false, new String[]{"Data Analytics", "MySQL", "Python"}},
                {"Solutions Consultant - Cloud Platforms", "Google", "DevOps & Cloud", EmploymentType.FULL_TIME, WorkplaceType.HYBRID, ExperienceLevel.EXPERIENCED, 5, 9, new BigDecimal("160000"), new BigDecimal("210000"), "Chicago, IL", false, new String[]{"Google Cloud", "Kubernetes", "Docker"}},
                {"Frontend Design Technologist", "Adobe", "UI/UX Design", EmploymentType.FULL_TIME, WorkplaceType.HYBRID, ExperienceLevel.MID_LEVEL, 3, 6, new BigDecimal("125000"), new BigDecimal("165000"), "San Francisco, CA", false, new String[]{"HTML5", "CSS3", "JavaScript", "React", "Figma"}}
        };

        List<Job> createdJobs = new ArrayList<>();
        int recruiterIdx = 0;

        for (Object[] jd : jobsCatalog) {
            String title = (String) jd[0];
            String compName = (String) jd[1];
            String category = (String) jd[2];
            EmploymentType empType = (EmploymentType) jd[3];
            WorkplaceType workType = (WorkplaceType) jd[4];
            ExperienceLevel expLevel = (ExperienceLevel) jd[5];
            int minExp = (Integer) jd[6];
            int maxExp = (Integer) jd[7];
            BigDecimal minSal = (BigDecimal) jd[8];
            BigDecimal maxSal = (BigDecimal) jd[9];
            String location = (String) jd[10];
            boolean isFeatured = (Boolean) jd[11];
            String[] reqSkills = (String[]) jd[12];

            Company company = companies.get(compName);
            Recruiter recruiter = recruitersList.get(recruiterIdx % recruitersList.size());
            recruiterIdx++;

            Job job = new Job();
            job.setTitle(title);
            job.setCompany(company);
            job.setRecruiter(recruiter);
            job.setJobCategory(category);
            job.setEmploymentType(empType);
            job.setWorkplaceType(workType);
            job.setExperienceLevel(expLevel);
            job.setMinExperienceYears(minExp);
            job.setMaxExperienceYears(maxExp);
            job.setMinSalary(minSal);
            job.setMaxSalary(maxSal);
            job.setLocation(location);
            job.setFeatured(isFeatured);
            job.setStatus(JobStatus.APPROVED);
            job.setDeadline(LocalDate.now().plusDays(45));
            job.setViewsCount((long) (45 + (createdJobs.size() * 17) % 350));

            job.setDescription("We are seeking an outstanding and dedicated " + title + " to join our engineering and product teams at " + compName + ". In this role, you will architect, implement, and scale critical mission systems serving global users. You will work closely with cross-functional partners in an agile, dynamic, and high-impact environment.");

            job.setResponsibilities("- Design, develop, test, deploy, and maintain high-performance software and systems.\n- Participate actively in code reviews, technical architecture sessions, and system design discussions.\n- Collaborate closely with Product Managers, UX Designers, and DevOps engineers.\n- Champion engineering excellence, clean code, automation, and operational reliability.");

            job.setRequirements("- Strong foundational background in computer science or equivalent practical experience.\n- Demonstrated proficiency with relevant technical stacks, modern frameworks, and cloud systems.\n- Excellent analytical, problem-solving, and communication capabilities.\n- Proven track record of shipping reliable software in an iterative environment.");

            job.setBenefits("- Highly competitive base salary with equity/stock options and annual performance bonus.\n- Comprehensive medical, dental, and vision insurance coverage.\n- 401(k) retirement plan with generous employer matching.\n- Flexible paid time off (PTO), parental leave, and wellness stipends.\n- Dedicated annual learning & development budget for conferences and certifications.");

            Set<Skill> jSkills = new HashSet<>();
            for (String s : reqSkills) {
                if (skillsMap.containsKey(s)) {
                    jSkills.add(skillsMap.get(s));
                }
            }
            job.setSkills(jSkills);

            job = jobRepository.save(job);
            createdJobs.add(job);
        }

        // 8. Seed Sample Applications and Saved Jobs for Alex Morgan (seeker@example.com)
        if (!seekersList.isEmpty() && !createdJobs.isEmpty()) {
            JobSeeker primarySeeker = seekersList.get(0);
            List<Resume> seekerResumes = resumeRepository.findByJobSeekerOrderByUploadedAtDesc(primarySeeker);
            Resume resume = seekerResumes.isEmpty() ? null : seekerResumes.get(0);

            // Apply for 4 jobs with different statuses
            ApplicationStatus[] statuses = {
                    ApplicationStatus.SHORTLISTED,
                    ApplicationStatus.UNDER_REVIEW,
                    ApplicationStatus.INTERVIEW,
                    ApplicationStatus.APPLIED
            };

            for (int i = 0; i < 4 && i < createdJobs.size(); i++) {
                Job job = createdJobs.get(i);
                if (!applicationRepository.existsByJobAndJobSeeker(job, primarySeeker)) {
                    Application app = new Application();
                    app.setJob(job);
                    app.setJobSeeker(primarySeeker);
                    app.setResume(resume);
                    app.setCoverLetter("Dear Hiring Team,\n\nI am thrilled to apply for the " + job.getTitle() + " position at " + job.getCompany().getName() + ". My expertise aligns directly with your mission and requirements. Thank you for your time and consideration.\n\nBest regards,\n" + primarySeeker.getUser().getFullName());
                    app.setStatus(statuses[i]);
                    if (statuses[i] == ApplicationStatus.SHORTLISTED) {
                        app.setRecruiterNotes("Strong Java and Spring Boot experience. Candidate profile matches all core requirements.");
                    } else if (statuses[i] == ApplicationStatus.INTERVIEW) {
                        app.setRecruiterNotes("Technical screen passed with flying colors. Advancing to architecture panel.");
                    }
                    applicationRepository.save(app);
                }
            }

            // Bookmark / Save 3 jobs
            for (int i = 4; i < 7 && i < createdJobs.size(); i++) {
                Job job = createdJobs.get(i);
                if (!savedJobRepository.existsByJobSeekerAndJob(primarySeeker, job)) {
                    savedJobRepository.save(new SavedJob(primarySeeker, job));
                }
            }
        }

        // 9. Applications from the other candidates so every recruiter has a pipeline to review
        ApplicationStatus[] pipeline = {
                ApplicationStatus.APPLIED, ApplicationStatus.UNDER_REVIEW, ApplicationStatus.SHORTLISTED,
                ApplicationStatus.INTERVIEW, ApplicationStatus.SELECTED, ApplicationStatus.REJECTED
        };
        for (int s = 1; s < seekersList.size(); s++) {
            JobSeeker candidate = seekersList.get(s);
            List<Resume> candidateResumes = resumeRepository.findByJobSeekerOrderByUploadedAtDesc(candidate);
            Resume candidateResume = candidateResumes.isEmpty() ? null : candidateResumes.get(0);
            for (int k = 0; k < 5; k++) {
                int jobIdx = (s * 3 + k * 2) % createdJobs.size();
                Job job = createdJobs.get(jobIdx);
                if (!applicationRepository.existsByJobAndJobSeeker(job, candidate)) {
                    Application app = new Application();
                    app.setJob(job);
                    app.setJobSeeker(candidate);
                    app.setResume(candidateResume);
                    app.setCoverLetter("Hello " + job.getCompany().getName() + " team,\n\nI'd love to bring my experience as a "
                            + candidate.getHeadline() + " to the " + job.getTitle() + " role. I've attached my resume and would welcome a conversation.\n\nKind regards,\n"
                            + candidate.getUser().getFullName());
                    app.setStatus(pipeline[(s + k) % pipeline.length]);
                    applicationRepository.save(app);
                }
            }
        }

        // 10. A couple of postings awaiting moderation, so the admin approval queue has content
        Object[][] pendingJobs = {
                {"Platform Engineer - Developer Experience", "Google", "Software Engineering", new String[]{"Kubernetes", "Docker", "CI/CD"}},
                {"Senior iOS Engineer", "Spotify", "Software Engineering", new String[]{"TypeScript", "REST APIs"}}
        };
        for (Object[] pj : pendingJobs) {
            String compName = (String) pj[1];
            Recruiter owner = recruitersList.stream()
                    .filter(r -> r.getCompany() != null && compName.equals(r.getCompany().getName()))
                    .findFirst().orElse(recruitersList.get(0));
            Job job = new Job();
            job.setTitle((String) pj[0]);
            job.setCompany(owner.getCompany());
            job.setRecruiter(owner);
            job.setJobCategory((String) pj[2]);
            job.setEmploymentType(EmploymentType.FULL_TIME);
            job.setWorkplaceType(WorkplaceType.HYBRID);
            job.setExperienceLevel(ExperienceLevel.EXPERIENCED);
            job.setMinExperienceYears(4);
            job.setMaxExperienceYears(8);
            job.setMinSalary(new BigDecimal("150000"));
            job.setMaxSalary(new BigDecimal("200000"));
            job.setLocation(owner.getCompany().getLocation());
            job.setStatus(JobStatus.PENDING);
            job.setDeadline(LocalDate.now().plusDays(30));
            job.setDescription("Join " + compName + " as a " + job.getTitle() + " and help shape the tools and platforms our teams rely on every day.");
            job.setRequirements("- 4+ years of relevant industry experience.\n- Strong communication and collaboration skills.");
            Set<Skill> pSkills = new HashSet<>();
            for (String s : (String[]) pj[3]) {
                if (skillsMap.containsKey(s)) pSkills.add(skillsMap.get(s));
            }
            job.setSkills(pSkills);
            jobRepository.save(job);
        }

        log.info("Database seeding completed successfully! Total jobs: {}, Total companies: {}",
                jobRepository.count(), companyRepository.count());
    }

    /**
     * Writes a small, valid one-page PDF so seeded resumes can actually be previewed and downloaded.
     */
    private Path writeSampleResumePdf(String storedName, String name, String headline, String email) {
        Path dir = Paths.get(resumeDir).toAbsolutePath().normalize();
        Path target = dir.resolve(storedName);
        String[] lines = {
                name,
                headline,
                email,
                "",
                "PROFESSIONAL SUMMARY",
                "Results-driven professional with a track record of shipping reliable,",
                "well-tested software and collaborating across product and design.",
                "",
                "EXPERIENCE",
                "Senior Engineer - Northwind Labs (2022 - Present)",
                "Software Engineer - Contoso Systems (2019 - 2022)",
                "",
                "EDUCATION",
                "Bachelor of Science, Computer Science",
                "",
                "This is a sample resume generated by JobConnect for demo purposes."
        };
        StringBuilder content = new StringBuilder("BT\n/F1 20 Tf\n72 740 Td\n");
        for (int i = 0; i < lines.length; i++) {
            if (i == 1) content.append("/F1 12 Tf\n");
            String safe = lines[i].replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
            content.append('(').append(safe).append(") Tj\n0 -20 Td\n");
        }
        content.append("ET\n");
        String stream = content.toString();

        List<String> objects = Arrays.asList(
                "<< /Type /Catalog /Pages 2 0 R >>",
                "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 5 0 R >> >> /Contents 4 0 R >>",
                "<< /Length " + stream.getBytes(StandardCharsets.ISO_8859_1).length + " >>\nstream\n" + stream + "endstream",
                "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>"
        );
        StringBuilder pdf = new StringBuilder("%PDF-1.4\n");
        List<Integer> offsets = new ArrayList<>();
        for (int i = 0; i < objects.size(); i++) {
            offsets.add(pdf.toString().getBytes(StandardCharsets.ISO_8859_1).length);
            pdf.append(i + 1).append(" 0 obj\n").append(objects.get(i)).append("\nendobj\n");
        }
        int xref = pdf.toString().getBytes(StandardCharsets.ISO_8859_1).length;
        pdf.append("xref\n0 ").append(objects.size() + 1).append("\n0000000000 65535 f \n");
        for (Integer off : offsets) {
            pdf.append(String.format("%010d 00000 n \n", off));
        }
        pdf.append("trailer\n<< /Size ").append(objects.size() + 1).append(" /Root 1 0 R >>\nstartxref\n")
                .append(xref).append("\n%%EOF\n");

        try {
            Files.createDirectories(dir);
            Files.write(target, pdf.toString().getBytes(StandardCharsets.ISO_8859_1));
        } catch (IOException e) {
            log.warn("Could not write sample resume {}: {}", target, e.getMessage());
        }
        return target;
    }
}
