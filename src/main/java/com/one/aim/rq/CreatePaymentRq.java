package com.one.aim.rq;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreatePaymentRq {
    private Long addressId;      //  ADD THIS
    private String paymentMethod; // ADD THIS (optional, you already know it's ONLINE)
}