package com.one.aim.repo;

import com.one.aim.bo.MarketingConversionBO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MarketingConversionRepo extends JpaRepository<MarketingConversionBO,Long> {

    @Query("""
        SELECT 
            mc.campaign.name,
            COUNT(mc.id),
            SUM(mc.revenueGenerated),
            0.0,
            MAX(c.code)
        FROM MarketingConversionBO mc
        LEFT JOIN mc.coupon c
        WHERE mc.createdAt BETWEEN :start AND :end
        GROUP BY mc.campaign.name
    """)
    Page<Object[]> getCampaignAggregates(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable
    );
}
