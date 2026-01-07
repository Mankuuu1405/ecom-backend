package com.one.aim.rs;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WishlistRs {

    private Long productId;
    private String productName;
    private String slug;
    private Double price;

    private boolean inStock;
    private boolean lowStock;

    //  ADD THESE TWO FIELDS
    private boolean available;
    private String message;

    private String productImageUrl;
    private String categoryName;
}

