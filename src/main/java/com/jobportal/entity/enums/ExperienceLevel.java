package com.jobportal.entity.enums;

public enum ExperienceLevel {
    FRESHER("Fresher (0-1 yrs)"),
    JUNIOR("Junior (1-3 yrs)"),
    MID_LEVEL("Mid Level (3-5 yrs)"),
    EXPERIENCED("Experienced (5-8 yrs)"),
    SENIOR("Senior (8+ yrs)");

    private final String displayName;

    ExperienceLevel(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
