package com.jobportal.entity.enums;

public enum JobStatus {
    PENDING("Pending Approval"),
    APPROVED("Active / Approved"),
    REJECTED("Rejected"),
    CLOSED("Closed");

    private final String displayName;

    JobStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
