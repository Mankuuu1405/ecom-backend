package com.one.aim.repo;

import com.one.aim.bo.ReviewBO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<ReviewBO, Long> {

    Page<ReviewBO> findByProduct_Slug(String slug, Pageable pageable);
    List<ReviewBO> findByProduct_Id(Long productId);
    Page<ReviewBO> findByProduct_Id(Long productId, Pageable pageable);
    Page<ReviewBO> findByUser_Id(Long userId, Pageable pageable);

    boolean existsByUser_IdAndProduct_Id(Long userId, Long productId);
    Optional<ReviewBO> findByUser_IdAndProduct_Id(Long userId, Long productId);

    @Query("""
SELECT AVG(r.rating)
FROM ReviewBO r
WHERE r.product.id = :productId
AND r.verified = true
""")
    Double getAverageRatingByProductId(Long productId);

    @Query("""
SELECT COUNT(r)
FROM ReviewBO r
WHERE r.product.id = :productId
AND r.verified = true
""")
    Long getReviewCountByProductId(Long productId);


    @Query("""
   SELECT r FROM ReviewBO r
   JOIN FETCH r.user
   WHERE r.rating >= 4
   ORDER BY r.createdAt DESC
""")
    Page<ReviewBO> findTopReviews(Pageable pageable);

    @Query("""
SELECT r.rating, COUNT(r)
FROM ReviewBO r
WHERE r.product.id = :productId
GROUP BY r.rating
""")
    List<Object[]> getRatingDistributionByProductId(@Param("productId") Long productId);


}

