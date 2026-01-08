package com.one.aim.repo;

import com.one.aim.bo.BannerBO;
import com.one.aim.constants.ContentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BannerRepo extends JpaRepository<BannerBO,Long> {
    Page<BannerBO> findByStatus(ContentStatus status, Pageable pageable);

    List<BannerBO> findByStatusAndPositionOrderByPriorityDesc(ContentStatus status, String position);

    @Query("SELECT b FROM BannerBO b WHERE b.status = :status AND " +
            "(b.scheduledPublishAt IS NULL OR b.scheduledPublishAt <= :now) AND " +
            "(b.scheduledUnpublishAt IS NULL OR b.scheduledUnpublishAt > :now)")
    List<BannerBO> findActiveBanners(@Param("status") ContentStatus status, @Param("now") LocalDateTime now);
}
