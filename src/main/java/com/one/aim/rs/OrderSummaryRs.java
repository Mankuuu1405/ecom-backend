package com.one.aim.rs;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderSummaryRs {

    private String orderId;

    private long subTotal;
    private long taxAmount;
    private long deliveryCharge;
    private long discountAmount;
    private long totalAmount;

    private String paymentMethod;
    private String paymentStatus;
    private String orderStatus;
    private LocalDateTime orderTime;

    private List<OrderItemRs> items;
}

