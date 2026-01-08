package com.one.aim.service;

import com.one.aim.constants.ContentStatus;
import com.one.aim.rq.PromotionRq;
import com.one.aim.rs.PagedRs;
import com.one.aim.rs.PromotionRs;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PromotionService {
    PromotionRs createPromotion(PromotionRq rq, MultipartFile image) throws Exception;

    PromotionRs updatePromotion(Long id, PromotionRq rq, MultipartFile image) throws Exception;

    PromotionRs getPromotionById(Long id);

    PagedRs<PromotionRs> getAllPromotions(Pageable pageable);

    PagedRs<PromotionRs> getPromotionsByStatus(ContentStatus status, Pageable pageable);

    List<PromotionRs> getActivePromotions();

    void deletePromotion(Long id);

    void processScheduledPromotions();
}
