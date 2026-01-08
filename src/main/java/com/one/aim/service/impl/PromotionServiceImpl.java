package com.one.aim.service.impl;

import com.one.aim.bo.FileBO;
import com.one.aim.bo.PromotionBO;
import com.one.aim.constants.ContentStatus;
import com.one.aim.mapper.PromotionMapper;
import com.one.aim.repo.PromotionRepository;
import com.one.aim.rq.PromotionRq;
import com.one.aim.rs.PagedRs;
import com.one.aim.rs.PromotionRs;
import com.one.aim.service.FileService;
import com.one.aim.service.PromotionService;
import com.one.exception.ResourceNotFoundException;
import com.one.utils.UrlUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromotionServiceImpl implements PromotionService {

    private final PromotionRepository promotionRepository;
    private final PromotionMapper promotionMapper;
    private final FileService fileService;
    private final UrlUtils urlUtils;

    @Override
    @Transactional
    public PromotionRs createPromotion(PromotionRq rq, MultipartFile image) throws Exception {
        // Handle image upload if provided
        if (image != null && !image.isEmpty()) {
            FileBO uploadedFile = fileService.uploadAndReturnFile(image);
            rq.setImageFileId(uploadedFile.getId());
        }

        PromotionBO promotion = promotionMapper.toEntity(rq);
        PromotionBO savedPromotion = promotionRepository.save(promotion);
        log.info("Promotion created with ID: {}", savedPromotion.getId());
        return toResponseWithImageUrl(savedPromotion);
    }

    @Override
    @Transactional
    public PromotionRs updatePromotion(Long id, PromotionRq rq, MultipartFile image) throws Exception {
        PromotionBO promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion not found with ID: " + id));

        // Handle new image upload if provided
        if (image != null && !image.isEmpty()) {
            // Delete old image if exists
            if (promotion.getImageFileId() != null) {
                try {
                    fileService.deleteFile(promotion.getImageFileId());
                } catch (Exception e) {
                    log.warn("Failed to delete old promotion image: {}", e.getMessage());
                }
            }

            // Upload new image
            FileBO uploadedFile = fileService.uploadAndReturnFile(image);
            rq.setImageFileId(uploadedFile.getId());
        }

        promotionMapper.updateEntityFromDTO(rq, promotion);
        PromotionBO updatedPromotion = promotionRepository.save(promotion);
        log.info("Promotion updated with ID: {}", updatedPromotion.getId());
        return toResponseWithImageUrl(updatedPromotion);
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionRs getPromotionById(Long id) {
        PromotionBO promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion not found with ID: " + id));
        return toResponseWithImageUrl(promotion);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedRs<PromotionRs> getAllPromotions(Pageable pageable) {
        Page<PromotionBO> promotionPage = promotionRepository.findAll(pageable);
        return buildPagedResponse(promotionPage);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedRs<PromotionRs> getPromotionsByStatus(ContentStatus status, Pageable pageable) {
        Page<PromotionBO> promotionPage = promotionRepository.findByStatus(status, pageable);
        return buildPagedResponse(promotionPage);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PromotionRs> getActivePromotions() {
        List<PromotionBO> promotions = promotionRepository.findActivePromotions(
                ContentStatus.PUBLISHED,
                LocalDateTime.now()
        );
        return promotions.stream()
                .map(this::toResponseWithImageUrl)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deletePromotion(Long id) {
        PromotionBO promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion not found with ID: " + id));

        // Delete associated image if exists
        if (promotion.getImageFileId() != null) {
            try {
                fileService.deleteFile(promotion.getImageFileId());
            } catch (Exception e) {
                log.warn("Failed to delete promotion image: {}", e.getMessage());
            }
        }

        promotionRepository.deleteById(id);
        log.info("Promotion deleted with ID: {}", id);
    }

    @Override
    @Transactional
    public void processScheduledPromotions() {
        LocalDateTime now = LocalDateTime.now();
        List<PromotionBO> allPromotions = promotionRepository.findAll();

        allPromotions.forEach(promotion -> {
            if (promotion.getStatus() == ContentStatus.SCHEDULED &&
                    promotion.getScheduledPublishAt() != null &&
                    promotion.getScheduledPublishAt().isBefore(now)) {
                promotion.setStatus(ContentStatus.PUBLISHED);
            }

            if (promotion.getStatus() == ContentStatus.PUBLISHED &&
                    promotion.getEndDate() != null &&
                    promotion.getEndDate().isBefore(now)) {
                promotion.setStatus(ContentStatus.ARCHIVED);
            }
        });

        promotionRepository.saveAll(allPromotions);
        log.info("Processed scheduled promotions");
    }

    private PromotionRs toResponseWithImageUrl(PromotionBO promotion) {
        PromotionRs rs = promotionMapper.toResponseDTO(promotion);
        if (promotion.getImageFileId() != null) {
            rs.setImageUrl(urlUtils.publicFile(promotion.getImageFileId()));
        } else {
            rs.setImageUrl(urlUtils.defaultImage());
        }
        return rs;
    }

    private PagedRs<PromotionRs> buildPagedResponse(Page<PromotionBO> page) {
        List<PromotionRs> content = page.getContent().stream()
                .map(this::toResponseWithImageUrl)
                .collect(Collectors.toList());

        return PagedRs.<PromotionRs>builder()
                .content(content)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .first(page.isFirst())
                .build();
    }
}