package com.one.aim.bo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "orders",
        indexes = {
                @Index(name = "idx_orders_user", columnList = "user_id"),
                @Index(name = "idx_orders_order_id", columnList = "orderId")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_orders_order_id", columnNames = "orderId")
        }
)
public class OrderBO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "orderId", unique = true, nullable = false, updatable = false, length = 20)
    private String orderId;

    private Long subTotal;
    private Long taxAmount;
    private Long deliveryCharge;
    private Long discountAmount;
    private Long paymentCharge;
    private Long totalAmount;

    private LocalDateTime orderTime;

    // PLACED, CONFIRMED, SHIPPED, DELIVERED, CANCELLED
    private String orderStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserBO user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id", nullable = false)
    private AddressBO shippingAddress;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItemBO> orderItems = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_person_id")
    private DeliveryPersonBO deliveryPerson;

    // COD, ONLINE
    private String paymentMethod;

    // ALWAYS "PAID" (payment-first architecture)
    private String paymentStatus;

    private String razorpayPaymentId;
    private String razorpaySignature;
    private String razorpayOrderId;

    private String invoiceno;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // ---------------------------------------------------------
    // Generate orderId before insert
    // ---------------------------------------------------------
    @PrePersist
    protected void onCreate() {
        if (this.orderTime == null) {
            this.orderTime = LocalDateTime.now();
        }
        if (this.orderId == null || this.orderId.isBlank()) {
            this.orderId = generateOrderCode();
        }
    }

    private String generateOrderCode() {
        String prefix = "ORD-";
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        StringBuilder sb = new StringBuilder(prefix);
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }
}



