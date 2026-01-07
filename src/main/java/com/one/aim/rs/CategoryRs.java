package com.one.aim.rs;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class CategoryRs {

    private Long id;
    private String name;
    private String slug;
    private boolean active;
    private String imageUrl;
    private boolean popular;
    private Double taxPercent;
    private Double deliveryCharge;
    private Integer returnPolicyDays;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;   // ADD THIS
}
