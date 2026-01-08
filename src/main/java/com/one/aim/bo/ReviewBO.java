package com.one.aim.bo;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;


@Entity
@Table(name = "reviews")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewBO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserBO user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductBO product;

    @Column(nullable = false)
    private Integer rating; // 1-5

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "likes", nullable = false)
    private Long likes = 0L;

    @Column(name = "dislikes", nullable = false)
    private Long dislikes = 0L;

    @Column(name = "verified", nullable = false)
    private boolean verified = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private boolean approved = false;


    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }


    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (likes == null) likes = 0L;
        if (dislikes == null) dislikes = 0L;
    }

}