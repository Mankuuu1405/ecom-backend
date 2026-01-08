package com.one.aim.constants;

public enum ContentType {
    BANNER("Banner"),
    BLOG_POST("Blog Post"),
    INFORMATIONAL_PAGE("Informational Page"),
    PROMOTION("Promotion");

    private final String displayName;

    ContentType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
