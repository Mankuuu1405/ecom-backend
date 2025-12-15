package com.one.aim.bo;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_events")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationEventBO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String type;          // ORDER_PLACED, PRODUCT_ADDED, SELLER_REGISTERED, SYSTEM

    private String title;
    private String description;

    private Long imageFileId;
    private String redirectUrl;
    private String targetRole;    // USER / SELLER / ADMIN / ALL

    @Column(name = "redirect_ref_id")
    private Long redirectRefId;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private OrderBO order;        //  (Purchase notification)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private ProductBO product;    //  (Product added/updated notification)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    private SellerBO seller;        //  (Seller registration notification)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "triggered_by_user_id")
    private UserBO triggeredByUser;  //  Who caused the event

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime expiryAt;
}


