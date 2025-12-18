package com.one.aim.rs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductDetailsRs {

    private Long id;
    private String name;
    private String description;
    private Double price;
    private Integer stock;
    private String brand;
    private String slug;

    private String categoryName;
    private Long categoryId;

    private boolean active;
    private boolean lowStock;
    private Boolean inStock;

    // =====================
    // IMAGES
    // =====================
    private List<String> images;   // images[0] is ALWAYS thumbnail
    private String thumbnail;      // Explicit thumbnail
    private Long thumbnailFileId;  // Optional, future-safe

    // Product specifications / attributes
    private Map<String, String> details;

    // Rating summary (AGGREGATE, not reviews)
    private Double averageRating;
    private Long reviewCount;
    private List<RatingDistributionRs> ratingDistribution;
}


