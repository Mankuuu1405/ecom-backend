package com.one.aim.rs;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class HomeSectionRs {

    private String title;
    private String type; // PRODUCTS / SERVICES / CATEGORIES / REVIEWS / BLOGS
    private List<?> items;
    private String viewAllUrl;

    // ===================== PRODUCTS =====================
    public static HomeSectionRs products(
            String title,
            List<ProductCardRs> items,
            String viewAllUrl
    ) {
        return HomeSectionRs.builder()
                .title(title)
                .type("PRODUCTS")
                .items(items)
                .viewAllUrl(viewAllUrl)
                .build();
    }

    // ===================== SERVICES =====================
    public static HomeSectionRs services(String title, List<?> items) {
        return HomeSectionRs.builder()
                .title(title)
                .type("SERVICES")
                .items(items)
                .build();
    }

    // ===================== CATEGORIES =====================
    public static HomeSectionRs categories(String title, List<CategoryCardRs> items) {
        return HomeSectionRs.builder()
                .title(title)
                .type("CATEGORIES")
                .items(items)
                .build();
    }

    // ===================== REVIEWS =====================
    public static HomeSectionRs reviews(String title, List<ReviewCardRs> items) {
        return HomeSectionRs.builder()
                .title(title)
                .type("REVIEWS")
                .items(items)
                .build();
    }

    // ===================== BLOGS =====================
    public static HomeSectionRs blogs(String title, List<BlogCardRs> items) {
        return HomeSectionRs.builder()
                .title(title)
                .type("BLOGS")
                .items(items)
                .build();
    }
}


