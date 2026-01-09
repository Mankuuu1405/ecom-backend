package com.one.aim.mapper;

import com.one.aim.bo.InformationalPageBO;
import com.one.aim.rq.InformationalPageRq;
import com.one.aim.rs.InformationalPageRs;
import org.springframework.stereotype.Component;

@Component
public class InformationalPageMapper {

    public InformationalPageBO toEntity(InformationalPageRq dto) {
        return InformationalPageBO.builder()
                .pageType(dto.getPageType())
                .title(generateTitle(dto))  // Auto-generate title
                .contentJson(dto.getContentJson())
                .status(dto.getStatus())
                .metaDescription(dto.getMetaDescription())
                .build();
    }

    public InformationalPageRs toResponseDTO(InformationalPageBO entity) {
        return InformationalPageRs.builder()
                .id(entity.getId())
                .pageType(entity.getPageType())
                .title(entity.getTitle())
                .contentJson(entity.getContentJson())
                .status(entity.getStatus())
                .metaDescription(entity.getMetaDescription())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public void updateEntityFromDTO(InformationalPageRq dto, InformationalPageBO entity) {
        entity.setPageType(dto.getPageType());
        entity.setTitle(generateTitle(dto));  // Auto-generate title on update
        entity.setContentJson(dto.getContentJson());
        entity.setStatus(dto.getStatus());
        entity.setMetaDescription(dto.getMetaDescription());
    }

    // Helper method to generate title
    private String generateTitle(InformationalPageRq dto) {
        // Use provided title if available, otherwise generate from pageType
        if (dto.getTitle() != null && !dto.getTitle().trim().isEmpty()) {
            return dto.getTitle();
        }
        return dto.getPageType().getDisplayName();
    }
}