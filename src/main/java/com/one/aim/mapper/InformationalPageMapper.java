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
                .title(dto.getTitle())
                .content(dto.getContent())
                .status(dto.getStatus())
                .metaDescription(dto.getMetaDescription())
                .build();
    }

    public InformationalPageRs toResponseDTO(InformationalPageBO entity) {
        return InformationalPageRs.builder()
                .id(entity.getId())
                .pageType(entity.getPageType())
                .title(entity.getTitle())
                .content(entity.getContent())
                .status(entity.getStatus())
                .metaDescription(entity.getMetaDescription())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public void updateEntityFromDTO(InformationalPageRq dto, InformationalPageBO entity) {
        entity.setPageType(dto.getPageType());
        entity.setTitle(dto.getTitle());
        entity.setContent(dto.getContent());
        entity.setStatus(dto.getStatus());
        entity.setMetaDescription(dto.getMetaDescription());
    }
}
