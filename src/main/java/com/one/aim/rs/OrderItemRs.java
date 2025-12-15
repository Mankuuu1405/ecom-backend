package com.one.aim.rs;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemRs {

    private Long productId;
    private String productName;
    private String productCategory;

    private Long sellerId;

    private Long unitPrice;
    private Integer quantity;
    private Long totalPrice;

    private String imageUrl;
}
