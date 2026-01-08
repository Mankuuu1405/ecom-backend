package com.one.aim.repo;

import com.one.aim.bo.ProductBO;
import com.one.aim.rs.BrandCountRs;
import com.one.aim.rs.PriceRangeRs;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public interface ProductRepo extends JpaRepository<ProductBO, Long>, JpaSpecificationExecutor<ProductBO> {

    Page<ProductBO> findByActiveTrue(Pageable pageable);


    List<ProductBO> findAllBySeller_Id(Long sellerId);

    Page<ProductBO> findByActiveTrueAndFeaturedTrueOrderByUpdatedAtDesc(Pageable pageable);


    Page<ProductBO> findByActiveTrueAndBestSellerTrue(Pageable pageable);

    Page<ProductBO> findByActiveTrueAndNewArrivalTrue(Pageable pageable);

    Page<ProductBO> findByActiveTrueAndOnSaleTrue(Pageable pageable);


    Optional<ProductBO> findBySlug(String slug);

    Page<ProductBO> findByCategoryNameIgnoreCase(String categoryName, Pageable pageable);

    Page<ProductBO> findByNameContainingIgnoreCase(String name, Pageable pageable);

    boolean existsBySlug(String slug);
    
    long count();

    @Query("""
    SELECT p
    FROM ProductBO p
    WHERE p.active = true
      AND p.createdAt >= :cutoff
    ORDER BY p.createdAt DESC
""")
    Page<ProductBO> findHomeNewArrivals(
            @Param("cutoff") LocalDateTime cutoff,
            Pageable pageable
    );



    Page<ProductBO> findByActiveTrueOrderByCreatedAtDesc(Pageable pageable);



    Page<ProductBO> findByCategoryId(Long categoryId, Pageable pageable);

    List<ProductBO> findBySellerId(Long sellerId);

//    @Query("SELECT COUNT(p) FROM ProductBO p WHERE p.seller.id = :sellerId")
//    Long countProductsBySeller(Long sellerId);

    Long countByCategoryIdAndActiveTrue(Long categoryId);



    Page<ProductBO> findByActiveTrueAndCategoryNameIgnoreCase(String category, Pageable pageable);

    Page<ProductBO> findByActiveTrueAndNameContainingIgnoreCase(String name, Pageable pageable);

    Page<ProductBO> findByActiveTrueAndNameContainingIgnoreCaseAndCategoryNameIgnoreCase(
            String name, String category, Pageable pageable
    );


    @Query("SELECT COUNT(p) FROM ProductBO p WHERE p.active = true AND p.categoryId = :categoryId")
    Long countActiveProductsByCategory(Long categoryId);

//    @Query("""
//    SELECT p FROM ProductBO p
//    WHERE p.active = true
//    AND (:categories IS NULL OR LOWER(p.categoryName) IN :categories)
//    AND (:brands IS NULL OR LOWER(p.brand) IN :brands)
//    AND (:minPrice IS NULL OR p.price >= :minPrice)
//    AND (:maxPrice IS NULL OR p.price <= :maxPrice)
//    AND (:rating IS NULL OR p.averageRating >= :rating)
//""")
//    Page<ProductBO> filterProducts(
//            @Param("categories") List<String> categories,
//            @Param("brands") List<String> brands,
//            Integer minPrice,
//            Integer maxPrice,
//            Integer rating,
//            Pageable pageable
//    );

    // ============================================
    // PRODUCT COUNT PER CATEGORY (ACTIVE ONLY)
    // ============================================
    @Query("""
        SELECT p.categoryId AS categoryId, COUNT(p.id) AS total
        FROM ProductBO p
        WHERE p.active = true
        GROUP BY p.categoryId
    """)
    List<Object[]> countProductsGroupedByCategory();

    default Map<Long, Long> countProductsGroupedByCategoryAsMap() {
        return countProductsGroupedByCategory().stream()
                .collect(Collectors.toMap(
                        r -> (Long) r[0],           // categoryId
                        r -> (Long) r[1]            // productCount
                ));
    }

    // ============================================
    // NEW ARRIVALS (HOMEPAGE)
    // ============================================
    @Query("""
    SELECT DISTINCT p 
    FROM ProductBO p
    LEFT JOIN FETCH p.imageFileIds
    WHERE p.active = true
    ORDER BY p.createdAt DESC
""")
    List<ProductBO> findNewArrivals(Pageable pageable);


    @Query("""
    SELECT DISTINCT p 
    FROM ProductBO p 
    LEFT JOIN FETCH p.imageFileIds 
    WHERE p.active = true AND p.featured = true
""")
    List<ProductBO> findFeaturedProducts(Pageable pageable);




    @Query("""
    SELECT p
    FROM OrderItemBO oi
    JOIN oi.product p
    WHERE oi.order.orderStatus = 'PLACED'
      AND oi.createdAt BETWEEN :start AND :end
      AND p.active = true
    GROUP BY p
    ORDER BY SUM(oi.quantity) DESC
""")
    Page<ProductBO> findTrendingProducts(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable
    );



    @Query("""
        SELECT p.categoryId, COUNT(p)
        FROM ProductBO p
        WHERE p.active = true
        GROUP BY p.categoryId
       """)
    List<Object[]> countProductsGrouped();

    @Query("""
    SELECT p FROM ProductBO p
    WHERE p.active = true
    AND (:categories IS NULL OR LOWER(p.categoryName) IN :categories)
    AND (:brands IS NULL OR LOWER(p.brand) IN :brands)
    AND (:minPrice IS NULL OR p.price >= :minPrice)
    AND (:maxPrice IS NULL OR p.price <= :maxPrice)
""")
    Page<ProductBO> filterProducts(
            @Param("categories") List<String> categories,
            @Param("brands") List<String> brands,
            @Param("minPrice") Integer minPrice,
            @Param("maxPrice") Integer maxPrice,
            @Param("rating") Integer rating,
            Pageable pageable
    );

    Page<ProductBO> findByActiveTrueAndCategoryNameIgnoreCaseAndIdNot(
            String categoryName,
            Long excludeId,
            Pageable pageable
    );


    Long countByCategoryId(Long categoryId);

    // Count active products by category name (for browse page)
    Long countByActiveTrueAndCategoryNameIgnoreCase(String categoryName);

    @Query("""
SELECT p FROM ProductBO p
WHERE p.active = true
AND p.createdAt >= :date
""")
    Page<ProductBO> findNewArrivals(
            @Param("date") LocalDateTime date,
            Pageable pageable
    );

    @Query("""
        SELECT COUNT(p)
        FROM ProductBO p
        WHERE p.seller.id = :sellerId
    """)
    Long countProductsBySeller(@Param("sellerId") Long sellerId);

    // -----------------------------
    // BRAND FACET
    // -----------------------------
    @Query("""
    SELECT new com.one.aim.rs.BrandCountRs(p.brand, COUNT(p))
    FROM ProductBO p
    WHERE p.active = true
      AND (:q IS NULL OR LOWER(p.name) LIKE %:q%)
      AND (:categoryId IS NULL OR p.categoryId = :categoryId)
    GROUP BY p.brand
""")
    List<BrandCountRs> getBrandFacets(
            @Param("q") String q,
            @Param("categoryId") Long categoryId
    );


    // -----------------------------
    // PRICE RANGE FACET
    // -----------------------------
    @Query("""
    SELECT new com.one.aim.rs.PriceRangeRs(
        MIN(p.price),
        MAX(p.price)
    )
    FROM ProductBO p
    WHERE p.active = true
      AND (:q IS NULL OR LOWER(p.name) LIKE %:q%)
      AND (:categoryId IS NULL OR p.categoryId = :categoryId)
""")
    PriceRangeRs getPriceRange(
            @Param("q") String q,
            @Param("categoryId") Long categoryId
    );


    // -----------------------------
    // AUTOCOMPLETE
    // -----------------------------
    @Query("""
        SELECT p.name
        FROM ProductBO p
        WHERE p.active = true
          AND LOWER(p.name) LIKE CONCAT(:q, '%')
        ORDER BY p.createdAt DESC
    """)
    List<String> autocompleteNames(
            @Param("q") String q,
            Pageable pageable
    );

    default List<String> autocompleteNames(String q) {
        return autocompleteNames(q, PageRequest.of(0, 8));
    }

    // =====================================================
    // BRAND FACETS
    // =====================================================
    @Query("""
    SELECT new com.one.aim.rs.BrandCountRs(
        p.brand,
        COUNT(p.id)
    )
    FROM ProductBO p
    WHERE p.active = true
      AND (:q IS NULL OR
           LOWER(p.name) LIKE CONCAT('%', :q, '%') OR
           LOWER(p.categoryName) LIKE CONCAT('%', :q, '%') OR
           LOWER(p.brand) LIKE CONCAT('%', :q, '%')
      )
      AND (:categoryIds IS NULL OR p.categoryId IN :categoryIds)
      AND (:minPrice IS NULL OR p.price >= :minPrice)
      AND (:maxPrice IS NULL OR p.price <= :maxPrice)
      AND (:rating IS NULL OR p.averageRating >= :rating)
    GROUP BY p.brand
    ORDER BY COUNT(p.id) DESC
""")
    List<BrandCountRs> getBrandFacets(
            String q,
            List<Long> categoryIds,
            Integer minPrice,
            Integer maxPrice,
            Integer rating
    );





    // =====================================================
    // PRICE RANGE FACET
    // =====================================================
    @Query("""
    SELECT new com.one.aim.rs.PriceRangeRs(
        MIN(p.price),
        MAX(p.price)
    )
    FROM ProductBO p
    WHERE p.active = true
      AND (:q IS NULL OR
           LOWER(p.name) LIKE CONCAT('%', :q, '%') OR
           LOWER(p.categoryName) LIKE CONCAT('%', :q, '%') OR
           LOWER(p.brand) LIKE CONCAT('%', :q, '%')
      )
      AND (:categoryIds IS NULL OR p.categoryId IN :categoryIds)
      AND (:brands IS NULL OR p.brand IN :brands)
      AND (:rating IS NULL OR p.averageRating >= :rating)
""")
    PriceRangeRs getPriceRange(
            String q,
            List<Long> categoryIds,
            List<String> brands,
            Integer rating
    );





    @Query("""
    SELECT p FROM ProductBO p
    WHERE p.active = true
      AND (
            :q IS NULL OR
            LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%')) OR
            LOWER(p.categoryName) LIKE LOWER(CONCAT('%', :q, '%')) OR
            LOWER(p.brand) LIKE LOWER(CONCAT('%', :q, '%'))
          )
      AND (:categoryIds IS NULL OR p.categoryId IN :categoryIds)
      AND (:brands IS NULL OR p.brand IN :brands)
      AND (:minPrice IS NULL OR p.price >= :minPrice)
      AND (:maxPrice IS NULL OR p.price <= :maxPrice)
      AND (:rating IS NULL OR p.averageRating >= :rating)
""")
    Page<ProductBO> search(
            @Param("q") String q,
            @Param("categoryIds") List<Long> categoryIds,
            @Param("brands") List<String> brands,
            @Param("minPrice") Integer minPrice,
            @Param("maxPrice") Integer maxPrice,
            @Param("rating") Integer rating,
            Pageable pageable
    );



}
