package com.one.aim.mapper;


import com.one.aim.bo.BannerBO;
import com.one.aim.rq.BannerRq;
import com.one.aim.rs.BannerRs;
import org.springframework.stereotype.Component;

@Component
public class BannerMapper {

    public BannerBO toEntity(BannerRq dto) {
        return BannerBO.builder()
                .title(dto.getTitle())
                .subtitle(dto.getSubtitle())
                .imageFileId(dto.getImageFileId())
                .buttonText(dto.getButtonText())
                .buttonLink(dto.getButtonLink())
                .status(dto.getStatus())
                .scheduledPublishAt(dto.getScheduledPublishAt())
                .scheduledUnpublishAt(dto.getScheduledUnpublishAt())
                .priority(dto.getPriority())
                .position(dto.getPosition())
                .build();
    }

    public BannerRs toResponseDTO(BannerBO entity) {
        return BannerRs.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .subtitle(entity.getSubtitle())
                .imageFileId(entity.getImageFileId())
                .buttonText(entity.getButtonText())
                .buttonLink(entity.getButtonLink())
                .status(entity.getStatus())
                .scheduledPublishAt(entity.getScheduledPublishAt())
                .scheduledUnpublishAt(entity.getScheduledUnpublishAt())
                .priority(entity.getPriority())
                .position(entity.getPosition())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public void updateEntityFromDTO(BannerRq dto, BannerBO entity) {
        entity.setTitle(dto.getTitle());
        entity.setSubtitle(dto.getSubtitle());
        entity.setImageFileId(dto.getImageFileId());
        entity.setButtonText(dto.getButtonText());
        entity.setButtonLink(dto.getButtonLink());
        entity.setStatus(dto.getStatus());
        entity.setScheduledPublishAt(dto.getScheduledPublishAt());
        entity.setScheduledUnpublishAt(dto.getScheduledUnpublishAt());
        entity.setPriority(dto.getPriority());
        entity.setPosition(dto.getPosition());
    }
}

