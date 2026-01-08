package com.one.aim.mapper;

import com.one.aim.bo.BlogBO;
import com.one.aim.rq.BlogCreateRq;
import com.one.aim.rs.BlogCardRs;
import org.springframework.stereotype.Component;

@Component
public class BlogMapper {

    public BlogBO toEntity(BlogCreateRq dto) {
        return BlogBO.builder()
                .title(dto.getTitle())
                .content(dto.getContent())
                .slug(dto.getSlug())
                .imageFileId(dto.getImageFileId())
                .status(dto.getStatus())
                .scheduledPublishAt(dto.getScheduledPublishAt())
                .metaDescription(dto.getMetaDescription())
                .keywords(dto.getKeywords())
                .author(dto.getAuthor())
                .build();
    }

    public BlogCardRs toResponseDTO(BlogBO entity) {
        return BlogCardRs.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .content(entity.getContent())
                .slug(entity.getSlug())
                .imageFileId(entity.getImageFileId())
                .status(entity.getStatus())
                .scheduledPublishAt(entity.getScheduledPublishAt())
                .metaDescription(entity.getMetaDescription())
                .keywords(entity.getKeywords())
                .author(entity.getAuthor())
                .viewCount(entity.getViewCount())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .publishedAt(entity.getPublishedAt())
                .build();
    }

    public void updateEntityFromDTO(BlogCreateRq dto, BlogBO entity) {
        entity.setTitle(dto.getTitle());
        entity.setContent(dto.getContent());
        entity.setSlug(dto.getSlug());
        entity.setImageFileId(dto.getImageFileId());
        entity.setStatus(dto.getStatus());
        entity.setScheduledPublishAt(dto.getScheduledPublishAt());
        entity.setMetaDescription(dto.getMetaDescription());
        entity.setKeywords(dto.getKeywords());
        entity.setAuthor(dto.getAuthor());
    }
}

