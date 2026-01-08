package com.one.aim.repo;

import com.one.aim.bo.PromotionBO;
import com.one.aim.constants.ContentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PromotionRepository extends JpaRepository<PromotionBO, Long>, JpaSpecificationExecutor<PromotionBO> {

    Page<PromotionBO> findByStatus(ContentStatus status, Pageable pageable);

    @Query("SELECT p FROM PromotionBO p WHERE p.status = :status AND " +
            "p.startDate <= :now AND p.endDate >= :now " +
            "ORDER BY p.priority DESC")
    List<PromotionBO> findActivePromotions(@Param("status") ContentStatus status, @Param("now") LocalDateTime now);
}
