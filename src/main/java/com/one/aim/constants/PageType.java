package com.one.aim.constants;

public enum PageType {
    ABOUT_US("About Us"),
    CONTACT_US("Contact Us"),
    FAQ("FAQ"),
    TERMS_CONDITIONS("Terms & Conditions"),
    PRIVACY_POLICY("Privacy Policy"),
    RETURN_POLICY("Return Policy"),
    SHIPPING_POLICY("Shipping Policy");

    private final String displayName;

    PageType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
