package com.one.aim.rs;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

import com.one.aim.bo.UserBO;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OrderRs implements Serializable {

    private static final long serialVersionUID = 1L;

    private String docId;
    private String orderId;

    //  PRICE BREAKDOWN
    private Long subTotal;
    private Long taxAmount;
    private Long deliveryCharge;
    private Long discountAmount;
    private Long paymentCharge;
    private Long totalAmount;

    //  ORDER META
    private String paymentMethod;
    private String paymentStatus;
    private LocalDateTime orderTime;
    private String orderStatus;

    //  RELATIONS
    private UserRs user;
    private AddressRs shippingAddress;
    private List<OrderItemRs> orderedItems;

}

