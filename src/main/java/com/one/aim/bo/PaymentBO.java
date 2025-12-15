package com.one.aim.bo;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "payment")
public class PaymentBO {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	private Long amount;

	private String paymentMethod; // PHONEPE / PAYTM

    @Column(name = "address_id")
    private Long addressId;

	private LocalDateTime paymentTime;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String status;   // PAID, FAILED


    @Column(name = "razorpay_signature")
    private String razorpaySignature;


    private String userid;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private UserBO user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private OrderBO order;

}
