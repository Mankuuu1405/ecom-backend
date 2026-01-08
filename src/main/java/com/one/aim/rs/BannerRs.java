package com.one.aim.rs;

import com.one.aim.constants.ContentStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BannerRs {
    private Long id;
    private String title;
    private String subtitle;
    private Long imageFileId;
    private String imageUrl;
    private String buttonText;
    private String buttonLink;
    private ContentStatus status;
    private LocalDateTime scheduledPublishAt;
    private LocalDateTime scheduledUnpublishAt;
    private Integer priority;
    private String position;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

