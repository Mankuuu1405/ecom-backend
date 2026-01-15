package com.one.aim.bo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "product",
        indexes = {@Index(name = "idx_product_seller", columnList = "seller_id")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class ProductBO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    private Double price;

    @Column(name = "offer_price")
    @DecimalMin(value = "0.01", message = "Offer price must be > 0")
    private Double offerPrice;

    @Column(name = "discount_percent")
    @Min(1)
    @Max(99)
    private Integer discountPercent;



    private Integer stock;

    private boolean active = true;

    private boolean lowStock = false;


    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    private SellerBO seller;


    //  Multiple image IDs (store FileBO IDs from FileService)
    @ElementCollection
    @CollectionTable(name = "product_images", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "image_file_id")
    private List<Long> imageFileIds = new ArrayList<>();

    private Long thumbnailFileId;


    //  Optional category text
    private String categoryName;

    private Long categoryId;

    @Column(nullable = false,name = "featured")
    private boolean featured = false;


    private String brand;

    @Column(name = "average_rating")
    private Double averageRating = 0.0;

    @Column(name = "review_count")
    private Long reviewCount = 0L;


    // Product Specifications/Details
    private String material;
    private String sole;
    private String closure;
    private String weight;
    private String color;
    private String size;

    // Additional specifications as JSON (flexible approach)
    @Column(columnDefinition = "TEXT")
    private String specificationsJson;

    private boolean bestSeller;
    private boolean newArrival;
    private boolean onSale;


    @Transient
    private Integer soldItem = 0;


    //  Unique slug or UUID for shareable links
    @Column(unique = true, nullable = false, updatable = false)
    private String slug;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

//    @PrePersist
//    protected void onCreate() {
//        if (slug == null || slug.isEmpty()) {
//            this.slug = UUID.randomUUID().toString();
//        }
//    }

    public void updateLowStock() {
        this.lowStock = (this.stock != null && this.stock <= 5);
    }

    public void ensureThumbnail() {
        if (this.thumbnailFileId == null
                && this.imageFileIds != null
                && !this.imageFileIds.isEmpty()) {

            this.thumbnailFileId = this.imageFileIds.get(0);
        }
    }

//    @PreUpdate
//    private void ensureThumbnailBeforeSave() {
//        ensureThumbnail();
//    }

    @PrePersist
    @PreUpdate
    private void beforeSave() {

        //  Ensure slug on create
        if (slug == null || slug.isEmpty()) {
            this.slug = UUID.randomUUID().toString();
        }

        ensureThumbnail();

        //  Normalize sale data
        if (!onSale) {
            offerPrice = null;
            discountPercent = null;
        }
    }




}
