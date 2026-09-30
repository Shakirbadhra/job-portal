package com.jobportal.entity.enums;

public enum WorkplaceType {
    REMOTE("Remote"),
    HYBRID("Hybrid"),
    ONSITE("On-site");

    private final String displayName;

    WorkplaceType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
