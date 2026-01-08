package com.one.aim.mapper;

import com.one.aim.bo.PromotionBO;
import com.one.aim.rq.PromotionRq;
import com.one.aim.rs.PromotionRs;
import org.springframework.stereotype.Component;

@Component
public class PromotionMapper {

    public PromotionBO toEntity(PromotionRq dto) {
        return PromotionBO.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .imageFileId(dto.getImageFileId())
                .discountPercentage(dto.getDiscountPercentage())
                .discountCode(dto.getDiscountCode())
                .status(dto.getStatus())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .scheduledPublishAt(dto.getScheduledPublishAt())
                .priority(dto.getPriority())
                .build();
    }

    public PromotionRs toResponseDTO(PromotionBO entity) {
        return PromotionRs.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .imageFileId(entity.getImageFileId())
                .discountPercentage(entity.getDiscountPercentage())
                .discountCode(entity.getDiscountCode())
                .status(entity.getStatus())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .scheduledPublishAt(entity.getScheduledPublishAt())
                .priority(entity.getPriority())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public void updateEntityFromDTO(PromotionRq dto, PromotionBO entity) {
        entity.setTitle(dto.getTitle());
        entity.setDescription(dto.getDescription());
        entity.setImageFileId(dto.getImageFileId());
        entity.setDiscountPercentage(dto.getDiscountPercentage());
        entity.setDiscountCode(dto.getDiscountCode());
        entity.setStatus(dto.getStatus());
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        entity.setScheduledPublishAt(dto.getScheduledPublishAt());
        entity.setPriority(dto.getPriority());
    }
}
