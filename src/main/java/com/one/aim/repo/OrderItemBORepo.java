package com.one.aim.repo;

import com.one.aim.bo.OrderItemBO;
import com.one.vm.analytics.TopProductVm;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderItemBORepo extends JpaRepository<OrderItemBO, Long> {

    //  Get total revenue for a date range
    @Query("""
        SELECT COALESCE(SUM(oi.totalPrice), 0)
        FROM OrderItemBO oi
        JOIN oi.order o
        WHERE o.orderTime BETWEEN :start AND :end
          AND o.orderStatus = 'DELIVERED'
    """)
    Long getTotalRevenue(LocalDateTime start, LocalDateTime end);

    //  Weekly revenue for (Week 1–4)
    @Query("""
        SELECT COALESCE(SUM(oi.totalPrice), 0)
        FROM OrderItemBO oi
        JOIN oi.order o
        WHERE o.orderTime BETWEEN :start AND :end
          AND o.orderStatus = 'DELIVERED'
    """)
    Long getRevenueBetween(LocalDateTime start, LocalDateTime end);

    //  Top-selling product (highest revenue)
    @Query("""
        SELECT oi.productName, SUM(oi.totalPrice)
        FROM OrderItemBO oi
        JOIN oi.order o
        WHERE o.orderTime BETWEEN :start AND :end
          AND o.orderStatus = 'DELIVERED'
        GROUP BY oi.productName
        ORDER BY SUM(oi.totalPrice) DESC
    """)
    List<Object[]> getTopSellingProduct(LocalDateTime start, LocalDateTime end);

    //  Sales table (Product, Category, Seller, Revenue)
    @Query("""
        SELECT 
            oi.productName,
            oi.productCategory,
            oi.sellerId,
            SUM(oi.totalPrice)
        FROM OrderItemBO oi
        JOIN oi.order o
        WHERE o.orderTime BETWEEN :start AND :end
          AND o.orderStatus = 'DELIVERED'
        GROUP BY oi.productName, oi.productCategory, oi.sellerId
        ORDER BY SUM(oi.totalPrice) DESC
    """)
    List<Object[]> getSalesTable(LocalDateTime start, LocalDateTime end);


    // Total seller revenue in date range
    @Query("""
           SELECT COALESCE(SUM(oi.totalPrice), 0)
           FROM OrderItemBO oi
           WHERE oi.sellerId = :sellerId
             AND oi.createdAt BETWEEN :start AND :end
           """)
    Long getSellerTotalRevenue(@Param("sellerId") Long sellerId,
                               @Param("start") LocalDateTime start,
                               @Param("end") LocalDateTime end);

    // Weekly revenue
    @Query("""
           SELECT COALESCE(SUM(oi.totalPrice), 0)
           FROM OrderItemBO oi
           WHERE oi.sellerId = :sellerId
             AND oi.createdAt BETWEEN :start AND :end
           """)
    Long getSellerRevenueBetween(@Param("sellerId") Long sellerId,
                                 @Param("start") LocalDateTime start,
                                 @Param("end") LocalDateTime end);

    // Product performance (last 30 days)
    @Query("""
           SELECT oi.productName,
                  oi.productCategory,
                  SUM(oi.quantity) AS totalQty,
                  SUM(oi.totalPrice) AS totalRevenue
           FROM OrderItemBO oi
           WHERE oi.sellerId = :sellerId
             AND oi.createdAt BETWEEN :start AND :end
           GROUP BY oi.productName, oi.productCategory
           ORDER BY totalRevenue DESC
           """)
    List<Object[]> getSellerProductPerformance(@Param("sellerId") Long sellerId,
                                               @Param("start") LocalDateTime start,
                                               @Param("end") LocalDateTime end);





    // Monthly revenue for full year (Jan–Dec)
    @Query("""
           SELECT 
               MONTH(oi.createdAt) AS month,
               SUM(oi.totalPrice)  AS revenue
           FROM OrderItemBO oi
           WHERE oi.sellerId = :sellerId
             AND YEAR(oi.createdAt) = :year
           GROUP BY MONTH(oi.createdAt)
           ORDER BY MONTH(oi.createdAt)
           """)
    List<Object[]> getSellerMonthlyRevenue(@Param("sellerId") Long sellerId,
                                           @Param("year") int year);


    @Query("""
    SELECT oi2.product.id, oi2.productName, SUM(oi2.quantity) AS freq
    FROM OrderItemBO oi1
    JOIN oi1.order o
    JOIN o.orderItems oi2
    WHERE oi1.product.id = :productId
      AND oi2.product.id <> :productId
      AND o.orderStatus = 'DELIVERED'
    GROUP BY oi2.product.id, oi2.productName
    ORDER BY freq DESC
""")
    List<Object[]> findFrequentlyBoughtTogether(@Param("productId") Long productId, Pageable pageable);


    @Query("""
    SELECT oi.product.id, oi.productName, SUM(oi.quantity) AS freq
    FROM OrderItemBO oi
    WHERE oi.order.user.id IN (
        SELECT o.user.id FROM OrderBO o
        JOIN o.orderItems oi1
        WHERE oi1.product.id = :productId
    )
    AND oi.product.id <> :productId
    GROUP BY oi.product.id, oi.productName
    ORDER BY freq DESC
""")
    List<Object[]> findPeopleAlsoBought(@Param("productId") Long productId, Pageable pageable);


    @Query("""
    SELECT oi.product.id, oi.productName, SUM(oi.quantity) AS qty
    FROM OrderItemBO oi
    WHERE oi.productCategory = :category
      AND oi.createdAt BETWEEN :start AND :end
      AND oi.order.orderStatus = 'DELIVERED'
    GROUP BY oi.product.id, oi.productName
    ORDER BY qty DESC
""")
    List<Object[]> findTopSellingByCategory(
            @Param("category") String category,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable
    );


//    @Query("""
//    SELECT oi.product.id, oi.productName, SUM(oi.quantity) AS qty
//    FROM OrderItemBO oi
//    WHERE oi.createdAt BETWEEN :start AND :end
//      AND oi.order.orderStatus = 'DELIVERED'
//    GROUP BY oi.product.id, oi.productName
//    ORDER BY qty DESC
//""")
//    List<Object[]> findTopSelling(
//            @Param("start") LocalDateTime start,
//            @Param("end") LocalDateTime end,
//            Pageable pageable
//    );


    @Query("""
    SELECT DISTINCT oi.product.id
    FROM OrderItemBO oi
    WHERE oi.order.user.id = :userId
""")
    List<Long> findProductIdsPurchasedByUser(@Param("userId") Long userId);



    // 1) Daily sales for seller (date, revenue)
    @Query("""
SELECT FUNCTION('DATE', oi.createdAt), COALESCE(SUM(oi.totalPrice),0)
FROM OrderItemBO oi
WHERE oi.sellerId = :sellerId
AND oi.createdAt BETWEEN :start AND :end
AND (:category IS NULL OR oi.productCategory = :category)
GROUP BY FUNCTION('DATE', oi.createdAt)
ORDER BY FUNCTION('DATE', oi.createdAt)
""")
    List<Object[]> getSellerDailySales(
            Long sellerId, LocalDateTime start, LocalDateTime end, String category
    );


    // 2) Daily unique order count for seller (date, orders)
    @Query("""
SELECT FUNCTION('DATE', oi.createdAt) AS day,
       COUNT(DISTINCT oi.order.id)
FROM OrderItemBO oi
WHERE oi.sellerId = :sellerId
  AND oi.createdAt BETWEEN :start AND :end
  AND (:category IS NULL OR oi.productCategory = :category)
GROUP BY FUNCTION('DATE', oi.createdAt)
ORDER BY FUNCTION('DATE', oi.createdAt)
""")
    List<Object[]> getSellerDailyOrderCount(
            @Param("sellerId") Long sellerId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("category") String category
    );


    // 3) Order status summary for seller (status, count of distinct orders)
    @Query("""
SELECT oi.order.orderStatus, COUNT(DISTINCT oi.order.id)
FROM OrderItemBO oi
WHERE oi.sellerId = :sellerId
  AND oi.createdAt BETWEEN :start AND :end
  AND oi.order.orderStatus IS NOT NULL
  AND (:category IS NULL OR oi.productCategory = :category)
GROUP BY oi.order.orderStatus
""")
    List<Object[]> getSellerOrderStatusSummary(
            @Param("sellerId") Long sellerId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("category") String category
    );


    @Query("SELECT COALESCE(SUM(oi.quantity), 0) FROM OrderItemBO oi WHERE oi.product.id = :productId")
    Integer countProductSales(@Param("productId") Long productId);


    @Query("""
SELECT p.id, p.name, SUM(oi.quantity)
FROM OrderItemBO oi
JOIN oi.product p
WHERE p.seller.id = :sellerId
AND oi.createdAt BETWEEN :start AND :end
AND (:category IS NULL OR oi.productCategory = :category)
GROUP BY p.id, p.name
ORDER BY SUM(oi.quantity) DESC
""")
    List<Object[]> findTopSellingBySeller(
            @Param("sellerId") Long sellerId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("category") String category,
            Pageable pageable
    );




    @Query("""
    SELECT p.id, p.name, SUM(oi.quantity)
    FROM OrderItemBO oi
    JOIN oi.product p
    WHERE oi.createdAt BETWEEN :start AND :end
    GROUP BY p.id, p.name
    ORDER BY SUM(oi.quantity) DESC
""")
    List<Object[]> findTopSelling(LocalDateTime start, LocalDateTime end, Pageable pageable);



//    @Query("""
//SELECT SUM(oi.totalPrice)
//FROM OrderItemBO oi
//WHERE oi.sellerId = :sellerId
//AND oi.createdAt BETWEEN :start AND :end
//""")
//    Double getTotalSalesBySeller(
//            @Param("sellerId") Long sellerId,
//            @Param("start") LocalDateTime start,
//            @Param("end") LocalDateTime end
//    );

//    @Query("""
//SELECT COUNT(DISTINCT oi.order.id)
//FROM OrderItemBO oi
//WHERE oi.sellerId = :sellerId
//AND oi.createdAt BETWEEN :start AND :end
//""")
//    Long getTotalOrdersBySeller(
//            @Param("sellerId") Long sellerId,
//            @Param("start") LocalDateTime start,
//            @Param("end") LocalDateTime end
//    );

//    @Query("""
//SELECT COUNT(DISTINCT oi.order.user.id)
//FROM OrderItemBO oi
//WHERE oi.sellerId = :sellerId
//AND oi.createdAt BETWEEN :start AND :end
//""")
//    Long getUniqueCustomersBySeller(
//            @Param("sellerId") Long sellerId,
//            @Param("start") LocalDateTime start,
//            @Param("end") LocalDateTime end
//    );

//    @Query(value = """
//SELECT
//ROUND(
//(COUNT(DISTINCT returning.user_id) * 100.0) /
//NULLIF(COUNT(DISTINCT all_users.user_id),0)
//)
//FROM order_items all_users
//LEFT JOIN order_items returning
//ON all_users.user_id = returning.user_id
//AND returning.created_at < :start
//WHERE all_users.seller_id = :sellerId
//AND all_users.created_at BETWEEN :start AND :end
//""", nativeQuery = true)
//    Integer getCustomerRetentionPercent(
//            @Param("sellerId") Long sellerId,
//            @Param("start") LocalDateTime start,
//            @Param("end") LocalDateTime end
//    );


    @Query("""
SELECT SUM(oi.totalPrice)
FROM OrderItemBO oi
WHERE oi.sellerId = :sellerId
AND oi.createdAt BETWEEN :start AND :end
""")
    Double getTotalSalesBySeller(
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
    Long getTotalOrdersBySeller(
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
    Long getUniqueCustomersBySeller(
            @Param("sellerId") Long sellerId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query(value = """
SELECT 
ROUND(
(COUNT(DISTINCT prev.user_id) * 100.0) /
NULLIF(COUNT(DISTINCT curr.user_id), 0)
)
FROM (
    SELECT DISTINCT o.user_id
    FROM order_items oi
    JOIN orders o ON oi.order_id = o.id
    WHERE oi.seller_id = :sellerId
      AND oi.created_at BETWEEN :start AND :end
) curr
LEFT JOIN (
    SELECT DISTINCT o.user_id
    FROM order_items oi
    JOIN orders o ON oi.order_id = o.id
    WHERE oi.seller_id = :sellerId
      AND oi.created_at < :start
) prev
ON curr.user_id = prev.user_id
""", nativeQuery = true)
    Integer getCustomerRetentionPercent(
            @Param("sellerId") Long sellerId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );



    @Query("""
SELECT SUM(oi.totalPrice)
FROM OrderItemBO oi
WHERE oi.sellerId = :sellerId
AND oi.createdAt BETWEEN :start AND :end
AND (:category IS NULL OR oi.productCategory = :category)
""")
    Double getTotalSalesBySeller(
            @Param("sellerId") Long sellerId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("category") String category
    );



    @Query("""
SELECT SUM(oi.totalPrice)
FROM OrderItemBO oi
WHERE oi.sellerId = :sellerId
AND oi.createdAt BETWEEN :start AND :end
AND (:category IS NULL OR oi.productCategory = :category)
""")
    Double getTotalSalesBySellerWithCategory(
            @Param("sellerId") Long sellerId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("category") String category
    );

    @Query("""
SELECT COUNT(DISTINCT oi.order.id)
FROM OrderItemBO oi
WHERE oi.sellerId = :sellerId
AND oi.createdAt BETWEEN :start AND :end
AND (:category IS NULL OR oi.productCategory = :category)
""")
    Long getTotalOrdersBySellerWithCategory(
            Long sellerId, LocalDateTime start, LocalDateTime end, String category
    );

    @Query("""
SELECT COUNT(DISTINCT oi.order.user.id)
FROM OrderItemBO oi
WHERE oi.sellerId = :sellerId
AND oi.createdAt BETWEEN :start AND :end
AND (:category IS NULL OR oi.productCategory = :category)
""")
    Long getUniqueCustomersBySellerWithCategory(
            Long sellerId, LocalDateTime start, LocalDateTime end, String category
    );


    @Query("""
    SELECT 
        oi.productName,
        oi.productCategory,
        oi.sellerId,
        SUM(oi.totalPrice)
    FROM OrderItemBO oi
    WHERE oi.createdAt BETWEEN :start AND :end
    GROUP BY oi.productName, oi.productCategory, oi.sellerId
""")
    Page<Object[]> getSalesTable(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable
    );

    @Query("""
    SELECT 
        oi.productName,
        SUM(oi.totalPrice),
        COUNT(DISTINCT oi.order.id),
        COUNT(DISTINCT oi.order.user.id)
    FROM OrderItemBO oi
    WHERE oi.createdAt BETWEEN :start AND :end
      AND (:category IS NULL OR oi.productCategory = :category)
      AND (:sellerId IS NULL OR oi.sellerId = :sellerId)
    GROUP BY oi.productName
""")
    Page<Object[]> getCustomReport(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("category") String category,
            @Param("sellerId") Long sellerId,
            Pageable pageable
    );



}

