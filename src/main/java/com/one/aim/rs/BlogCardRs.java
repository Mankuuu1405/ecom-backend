package com.one.aim.rs;

import com.one.aim.constants.ContentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class BlogCardRs {
    private Long id;
    private String title;
    private String content;
    private String slug;
    private Long imageFileId;
    private String imageUrl;
    private ContentStatus status;
    private LocalDateTime scheduledPublishAt;
    private String metaDescription;
    private String keywords;
    private String author;
    private Long viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime publishedAt;
}