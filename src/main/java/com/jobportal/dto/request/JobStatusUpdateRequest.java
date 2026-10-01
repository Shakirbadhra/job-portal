package com.jobportal.dto.request;

import com.jobportal.entity.enums.JobStatus;

import javax.validation.constraints.NotNull;

public class JobStatusUpdateRequest {

    @NotNull(message = "Job status is required")
    private JobStatus status;

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }
}
