package com.one.aim.constants;

public enum PageType {

    // ================= CUSTOMER SERVICE =================
    HELP_CENTER("Help Center"),
    FAQ("Frequently Asked Questions"),
    RETURNS_REFUNDS("Returns & Refunds"),
    SHIPPING_POLICY("Shipping Information"),
    CONTACT_US("Contact Us"),

    // ================= ABOUT & LEGAL =================
    ABOUT_US("About Us"),
    SELLER_TERMS("Seller Terms"),
    CAREERS("Careers"),
    PRESS_CENTER("Press Center"),
    TERMS_OF_SERVICE("Terms of Service"),
    PRIVACY_POLICY("Privacy Policy"),
    COOKIE_POLICY("Cookie Policy"),
    ACCESSIBILITY("Accessibility"),
    SITEMAP("Sitemap"),
    AFFILIATE_PROGRAM("Affiliate Program");

    private final String displayName;

    PageType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getRoutePath() {
        switch (this) {
            case FAQ:
                return "/faq";
            case HELP_CENTER:
                return "/help-center";
            case RETURNS_REFUNDS:
                return "/returns-refunds";
            case SHIPPING_POLICY:
                return "/shipping";
            case CONTACT_US:
                return "/contact";
            case ABOUT_US:
                return "/about";
            case CAREERS:
                return "/careers";
            case PRESS_CENTER:
                return "/press";
            case TERMS_OF_SERVICE:
                return "/terms";
            case PRIVACY_POLICY:
                return "/privacy";
            case COOKIE_POLICY:
                return "/cookie";
            case ACCESSIBILITY:
                return "/accessibility";
            case SITEMAP:
                return "/sitemap";
            case AFFILIATE_PROGRAM:
                return "/affiliate-program";
            case SELLER_TERMS:
                return "/seller-terms";

            default:
                return "/" + this.name().toLowerCase().replace("_", "-");
        }
    }
}
