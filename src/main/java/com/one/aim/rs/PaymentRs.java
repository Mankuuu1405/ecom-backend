package com.one.aim.rs;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
public class PaymentRs implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long amount;                 // rupees
    private String paymentMethod;
    private String status;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private LocalDateTime paymentTime;
}
