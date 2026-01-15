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

    /* IMAGES */
    private List<String> images;
    private String thumbnail;

    /* DETAILS */
    private Map<String, String> details;

    /*  FLATTENED RATING SUMMARY */
    private Double averageRating;              // 3.0
    private Long reviewCount;                  // 1
    private List<RatingDistributionRs> ratingDistribution;

    private Boolean onSale;
    private Double offerPrice;        // discounted selling price (₹)
    private Integer discountPercent;  // discount % (auto OR manual)
}

