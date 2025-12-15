package com.one.aim.rs;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class HomeSectionRs {

    private String title;
    private String type; // PRODUCTS / SERVICES / CATEGORIES
    private List<?> items;
    private String viewAllUrl;


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


    public static HomeSectionRs services(String title, List<ServiceCardRs> list) {
        return HomeSectionRs.builder()
                .title(title)
                .type("SERVICES")
                .items(list)
                .build();
    }

    public static HomeSectionRs categories(String title, List<CategoryCardRs> list) {
        return HomeSectionRs.builder()
                .title(title)
                .type("CATEGORIES")
                .items(list)
                .build();
    }
}

