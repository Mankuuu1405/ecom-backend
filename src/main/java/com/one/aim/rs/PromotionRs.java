package com.one.aim.rs;

import com.one.aim.constants.ContentStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionRs {
    private Long id;
    private String title;
    private String description;
    private Long imageFileId;
    private String imageUrl;
    private Integer discountPercentage;
    private String discountCode;
    private ContentStatus status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime scheduledPublishAt;
    private Integer priority;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
