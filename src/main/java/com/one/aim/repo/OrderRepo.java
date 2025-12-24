package com.one.aim.repo;

import com.one.aim.bo.AddressBO;
import com.one.aim.bo.OrderBO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepo extends JpaRepository<OrderBO, Long> {

    // ==========================================================
    // BASIC FINDERS
    // ==========================================================
    List<OrderBO> findByUser_Id(Long userId);

    OrderBO findByRazorpayOrderId(String razorpayOrderId);

    OrderBO findByInvoiceno(String invoiceno);

    Optional<OrderBO> findByOrderId(String orderId);

    @Query("""
        SELECT COUNT(o) > 0
        FROM OrderBO o
        JOIN o.orderItems oi
        WHERE o.user.id = :userId
          AND oi.product.id = :productId
          AND o.orderStatus = 'DELIVERED'
    """)
    boolean hasUserPurchasedProduct(
            @Param("userId") Long userId,
            @Param("productId") Long productId
    );



    // ==========================================================
    // SELLER — Orders Containing Seller Products
    // ==========================================================
    @Query("""
        SELECT DISTINCT o
        FROM OrderBO o
        JOIN o.orderItems oi
        JOIN oi.product p
        WHERE p.seller.id = :sellerId
    """)
    List<OrderBO> findOrdersBySellerId(@Param("sellerId") Long sellerId);


    // ==========================================================
    // SELLER DASHBOARD ANALYTICS
    // ==========================================================

    // Total Sales
    @Query("""
        SELECT SUM(oi.totalPrice)
        FROM OrderItemBO oi
        WHERE oi.sellerId = :sellerId
    """)
    Long getTotalSalesBySeller(@Param("sellerId") Long sellerId);


    // Total Orders
    @Query("""
        SELECT COUNT(DISTINCT oi.order.id)
        FROM OrderItemBO oi
        WHERE oi.sellerId = :sellerId
    """)
    Long countBySellerId(@Param("sellerId") Long sellerId);


    // Last Month Sales
    @Query("""
        SELECT SUM(oi.totalPrice)
        FROM OrderItemBO oi
        WHERE oi.sellerId = :sellerId
          AND oi.createdAt >= :startDate
    """)
    Long getLastMonthSalesBySeller(
            @Param("sellerId") Long sellerId,
            @Param("startDate") LocalDateTime startDate
    );


    // Last Month Order Count
    @Query("""
        SELECT COUNT(DISTINCT oi.order.id)
        FROM OrderItemBO oi
        WHERE oi.sellerId = :sellerId
          AND oi.createdAt >= :startDate
    """)
    Long getLastMonthOrderCountBySeller(
            @Param("sellerId") Long sellerId,
            @Param("startDate") LocalDateTime startDate
    );


    // Recent Orders
    @Query("""
        SELECT o.id, u.fullName, o.createdAt, o.orderStatus, o.totalAmount
        FROM OrderBO o
        JOIN o.user u
        WHERE o.id IN (
            SELECT DISTINCT oi.order.id
            FROM OrderItemBO oi
            WHERE oi.sellerId = :sellerId
        )
        ORDER BY o.createdAt DESC
    """)
    List<Object[]> findRecentOrders(@Param("sellerId") Long sellerId);


    // Sales By Product Per Day
    @Query("""
        SELECT oi.productName, DATE(oi.createdAt), SUM(oi.totalPrice)
        FROM OrderItemBO oi
        WHERE oi.sellerId = :sellerId
        GROUP BY oi.productName, DATE(oi.createdAt)
        ORDER BY oi.productName ASC
    """)
    List<Object[]> getSalesByProductPerDay(@Param("sellerId") Long sellerId);


    // Today's Sales
    @Query("""
        SELECT SUM(oi.totalPrice)
        FROM OrderItemBO oi
        WHERE oi.sellerId = :sellerId
          AND DATE(oi.createdAt) = CURRENT_DATE
    """)
    Double getTodaySales(@Param("sellerId") Long sellerId);


    // ==========================================================
    // ADDITIONAL SELLER ANALYTICS
    // ==========================================================

    @Query("""
        SELECT COUNT(DISTINCT oi.order.id)
        FROM OrderItemBO oi
        WHERE oi.sellerId = :sellerId
          AND oi.createdAt BETWEEN :start AND :end
    """)
    Long countSellerOrders(
            @Param("sellerId") Long sellerId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );


    @Query("""
        SELECT COUNT(DISTINCT oi.order.user.id)
        FROM OrderItemBO oi
        WHERE oi.sellerId = :sellerId
          AND oi.createdAt BETWEEN :start AND :end
    """)
    Long countSellerUniqueCustomers(
            @Param("sellerId") Long sellerId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );


    @Query("""
        SELECT COUNT(DISTINCT oi.order.user.id)
        FROM OrderItemBO oi
        WHERE oi.sellerId = :sellerId
          AND oi.createdAt BETWEEN :start AND :end
          AND oi.order.user.id IN (
              SELECT oi2.order.user.id
              FROM OrderItemBO oi2
              WHERE oi2.sellerId = :sellerId
                AND oi2.createdAt < :start
          )
    """)
    Long countSellerReturningCustomers(
            @Param("sellerId") Long sellerId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );


    // ==========================================================
    // ORDER VOLUME
    // ==========================================================
    @Query("""
        SELECT COUNT(o)
        FROM OrderBO o
        WHERE o.createdAt BETWEEN :start AND :end
    """)
    Long getOrderVolume(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );


    // ==========================================================
    // ADMIN DASHBOARD
    // ==========================================================
    @Query("""
        SELECT SUM(o.totalAmount)
        FROM OrderBO o
    """)
    Long getTotalRevenue();


    @Query("""
        SELECT MONTH(o.createdAt), SUM(o.totalAmount)
        FROM OrderBO o
        WHERE o.createdAt >= :startDate
        GROUP BY MONTH(o.createdAt)
        ORDER BY MONTH(o.createdAt)
    """)
    List<Object[]> getRevenueByMonth(@Param("startDate") LocalDateTime startDate);


    @Query("""
        SELECT o.orderStatus, COUNT(o)
        FROM OrderBO o
        GROUP BY o.orderStatus
    """)
    List<Object[]> getOrderDistribution();


    boolean existsByShippingAddress(AddressBO address);


    // ==========================================================
    // SELLER HELPERS
    // ==========================================================
    List<OrderBO> findAllByUser_IdOrderByOrderTimeDesc(Long userId);

    Page<OrderBO> findByOrderStatusIgnoreCase(String status, Pageable pageable);

    @Query("""
        SELECT DISTINCT o
        FROM OrderBO o
        JOIN o.orderItems oi
        WHERE oi.sellerId = :sellerId
          AND (:status IS NULL OR o.orderStatus = :status)
    """)
    Page<OrderBO> findOrdersForSeller(
            @Param("sellerId") Long sellerId,
            @Param("status") String status,
            Pageable pageable
    );


    @Query("""
        SELECT CASE WHEN COUNT(oi) > 0 THEN TRUE ELSE FALSE END
        FROM OrderItemBO oi
        WHERE oi.order.orderId = :orderId
          AND oi.sellerId = :sellerId
    """)
    boolean sellerOwnsOrder(
            @Param("orderId") String orderId,
            @Param("sellerId") Long sellerId
    );

    @Query(value = """
    SELECT oi.product_name, SUM(oi.quantity)
    FROM order_items oi
    WHERE oi.seller_id = :sellerId
    GROUP BY oi.product_name
    ORDER BY SUM(oi.quantity) DESC
""", nativeQuery = true)
    List<Object[]> getTopProducts(@Param("sellerId") Long sellerId);

    @Query("""
    SELECT COALESCE(SUM(oi.totalPrice), 0)
    FROM OrderItemBO oi
    WHERE oi.sellerId = :sellerId
""")
    Long getTotalRevenueBySeller(@Param("sellerId") Long sellerId);


    @Query("""
    SELECT COUNT(DISTINCT oi.order.id)
    FROM OrderItemBO oi
    WHERE oi.sellerId = :sellerId
""")
    Long getSellerOrderCount(@Param("sellerId") Long sellerId);


    @Query("""
    SELECT COUNT(DISTINCT oi.order.user.id)
    FROM OrderItemBO oi
    WHERE oi.createdAt BETWEEN :start AND :end
""")
    Long getActiveUsers(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
    SELECT o.orderId, u.fullName, o.createdAt, o.orderStatus, o.totalAmount
    FROM OrderBO o
    JOIN o.user u
    WHERE o.id IN (
        SELECT DISTINCT oi.order.id
        FROM OrderItemBO oi
        WHERE oi.sellerId = :sellerId
    )
    ORDER BY o.createdAt DESC
""")
    Page<Object[]> findRecentOrders(
            @Param("sellerId") Long sellerId,
            Pageable pageable
    );



    @Query("""
    SELECT COALESCE(SUM(oi.totalPrice), 0)
    FROM OrderItemBO oi
    WHERE oi.sellerId = :sellerId
      AND oi.createdAt BETWEEN :start AND :end
""")
    Double getRevenueBetween(
            @Param("sellerId") Long sellerId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );


    @Query("""
    SELECT COUNT(DISTINCT oi.order.id)
    FROM OrderItemBO oi
    WHERE oi.sellerId = :sellerId
      AND oi.createdAt BETWEEN :start AND :end
""")
    Long getOrdersBetween(
            @Param("sellerId") Long sellerId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );




}

