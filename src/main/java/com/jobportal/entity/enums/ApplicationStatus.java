package com.jobportal.entity.enums;

public enum ApplicationStatus {
    APPLIED("Applied", "badge-primary", "#3b82f6"),
    UNDER_REVIEW("Under Review", "badge-info", "#06b6d4"),
    SHORTLISTED("Shortlisted", "badge-warning", "#f59e0b"),
    INTERVIEW("Interview", "badge-purple", "#8b5cf6"),
    SELECTED("Selected", "badge-success", "#10b981"),
    REJECTED("Rejected", "badge-danger", "#ef4444");

    private final String displayName;
    private final String badgeClass;
    private final String colorCode;

    ApplicationStatus(String displayName, String badgeClass, String colorCode) {
        this.displayName = displayName;
        this.badgeClass = badgeClass;
        this.colorCode = colorCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBadgeClass() {
        return badgeClass;
    }

    public String getColorCode() {
        return colorCode;
    }
}
