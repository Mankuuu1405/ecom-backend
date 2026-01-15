package com.one.aim.rs;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ProductRs implements Serializable {

    private static final long serialVersionUID = 1L;

    private String docId;
    private String name;
    private String slug;
    private String description;
    private Double price;
    private Integer stock;
    private String brand;
    private String categoryName;
    private Long categoryId;

    // =====================
    // IMAGES
    // =====================
    private String image;              // ALWAYS same as thumbnail
    private String thumbnail;          // Explicit thumbnail
    private Long thumbnailFileId;      // For seller edit UI
    private List<String> images;       // Thumbnail is always images[0]

    private boolean inStock;
    private boolean active;
    private boolean featured;

    // Product details
    private String material;
    private String sole;
    private String closure;
    private String weight;
    private String color;
    private String size;
    private String specificationsJson;

    // Ratings
    private Double averageRating;
    private Long reviewCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Boolean onSale;
    private Double offerPrice;        // discounted selling price (₹)
    private Integer discountPercent;  // discount % (auto OR manual)
}
