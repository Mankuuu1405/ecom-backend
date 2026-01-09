package com.one.aim.constants;

public enum PageType {
    FAQ("Frequently Asked Questions"),
    ABOUT_US("About Us"),
    CONTACT_US("Contact Us"),
    TERMS_CONDITIONS("Terms & Conditions"),
    PRIVACY_POLICY("Privacy Policy"),
    RETURN_POLICY("Return Policy"),
    SHIPPING_POLICY("Shipping Policy"),
    COOKIE_POLICY("Cookie Policy"),
    CAREERS("Careers"),
    HELP_CENTER("Help Center");

    private final String displayName;

    PageType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get the frontend route path for this page type
     */
    public String getRoutePath() {
        switch (this) {
            case FAQ:
                return "/faq";
            case ABOUT_US:
                return "/about";
            case CONTACT_US:
                return "/contact";
            case TERMS_CONDITIONS:
                return "/terms";
            case PRIVACY_POLICY:
                return "/privacy";
            case RETURN_POLICY:
                return "/returns";
            case SHIPPING_POLICY:
                return "/shipping";
            case COOKIE_POLICY:
                return "/cookie";
            case CAREERS:
                return "/careers";
            case HELP_CENTER:
                return "/help-center";
            default:
                return "/" + this.name().toLowerCase().replace("_", "-");
        }
    }
}
