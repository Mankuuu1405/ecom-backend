package com.one.aim.rs;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
public class ProductCardRs {

    private String docId;
    private String name;
    private String slug;
    private String brand;              // NEW: Brand name
    private Double price;
    private String categoryName;
    private Long categoryId;
    private String image;              // First image URL
    private boolean inStock;
    private String shortDescription;   // NEW: Short description (max 100 chars)
    private Double averageRating;
    private Long reviewCount;
}

