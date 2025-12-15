package com.one.aim.repo;

import com.one.aim.bo.ReviewBO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<ReviewBO, Long> {

    // =========================
    // BASIC REVIEW FETCHING
    // =========================

    List<ReviewBO> findByProduct_Id(Long productId);

    Page<ReviewBO> findByProduct_Id(Long productId, Pageable pageable);

    Page<ReviewBO> findByUser_Id(Long userId, Pageable pageable);

    boolean existsByUser_IdAndProduct_Id(Long userId, Long productId);

    // =========================
    // RATING AGGREGATES
    // =========================

    @Query("SELECT AVG(r.rating) FROM ReviewBO r WHERE r.product.id = :productId")
    Double getAverageRatingByProductId(@Param("productId") Long productId);

    @Query("SELECT COUNT(r) FROM ReviewBO r WHERE r.product.id = :productId")
    Long getReviewCountByProductId(@Param("productId") Long productId);


    @Query("""
SELECT r.rating, COUNT(r)
FROM ReviewBO r
WHERE r.product.id = :productId
GROUP BY r.rating
""")
    List<Object[]> getRatingDistributionByProductId(@Param("productId") Long productId);

}
