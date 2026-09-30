package com.jobportal.dto.request;

import com.jobportal.entity.enums.ApplicationStatus;

import javax.validation.constraints.NotNull;

public class ApplicationStatusUpdateRequest {

    @NotNull(message = "Application status is required")
    private ApplicationStatus status;

    private String recruiterNotes;

    public ApplicationStatusUpdateRequest() {}

    public ApplicationStatusUpdateRequest(ApplicationStatus status, String recruiterNotes) {
        this.status = status;
        this.recruiterNotes = recruiterNotes;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }

    public String getRecruiterNotes() {
        return recruiterNotes;
    }

    public void setRecruiterNotes(String recruiterNotes) {
        this.recruiterNotes = recruiterNotes;
    }
}
