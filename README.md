# JobConnect — Job Portal

A full-stack job portal built with **Spring Boot 2.7**, **Spring Security**, **Spring Data JPA** and **Thymeleaf**.
Candidates search and apply for jobs, recruiters post roles and manage applicants, and admins moderate the platform.

## Features

**Candidates**
- Search jobs by keyword, location, category, job type, workplace, experience, salary and company
- Apply with a saved or newly uploaded resume (PDF/DOC/DOCX) and an optional cover letter
- Track every application through Applied → Review → Shortlist → Interview → Hired, with recruiter notes
- Save jobs, manage multiple resumes, edit profile and skills, change password

**Recruiters**
- Post, edit, close, reopen and delete jobs
- Review applicants with an inline resume preview and update their status
- Maintain a public company profile

**Admins**
- Dashboard with charts for applications and jobs
- Approve, reject, feature or remove job posts
- Enable or disable user accounts
- Read messages sent from the Contact page

## Getting started

Requirements: **Java 8+** (tested on Java 21) and **Maven 3.6+**.

```bash
mvn spring-boot:run
```

Open <http://localhost:8080>. Run on another port with `mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8090`.

The app uses an embedded **H2** database stored in `./data`, so no database setup is needed. Demo data is loaded on first start.

### Using MySQL instead

Create a MySQL user (defaults: `root` / `root`, see `src/main/resources/application-mysql.properties`), then run:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

The `jobportal_db` database is created automatically.

## Demo accounts

| Role      | Email                  | Password      |
|-----------|------------------------|---------------|
| Candidate | `seeker@example.com`   | `Seeker@123`  |
| Recruiter | `recruiter@google.com` | `Recruiter@123` |
| Admin     | `admin@jobconnect.com` | `Admin@123`   |

Change these passwords before deploying anywhere public.

## Configuration

| Property | Default | Purpose |
|----------|---------|---------|
| `jobportal.jobs.require-approval` | `false` | When `true`, new jobs start as *Pending* until an admin approves them |
| `jobportal.upload.resume-dir` | `uploads/resumes` | Where uploaded resumes are stored |
| `spring.servlet.multipart.max-file-size` | `10MB` | Maximum resume size |

## Project structure

```
src/main/java/com/jobportal
├── config/        Demo data seeding
├── controller/
│   ├── api/       REST endpoints (/api/**)
│   └── page/      Thymeleaf page controllers
├── dto/           Request and response objects
├── entity/        JPA entities
├── exception/     Error handling for API and pages
├── repository/    Spring Data repositories and search specification
├── security/      Spring Security configuration
└── service/       Business logic
src/main/resources
├── templates/     public/, seeker/, recruiter/, admin/, error/ and shared fragments/
└── static/        css/custom.css and js/
```
