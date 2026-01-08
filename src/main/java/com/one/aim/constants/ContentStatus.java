package com.one.aim.constants;

public enum ContentStatus {
    DRAFT("Draft"),
    PUBLISHED("Published"),
    SCHEDULED("Scheduled"),
    ARCHIVED("Archived");

    private final String displayName;

    ContentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}


