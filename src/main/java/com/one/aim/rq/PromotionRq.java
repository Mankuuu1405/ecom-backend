package com.one.aim.rq;

import com.one.aim.constants.ContentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionRq {

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    private String description;

    private Long imageFileId;

    private Integer discountPercentage;

    @Size(max = 50, message = "Discount code must not exceed 50 characters")
    private String discountCode;

    private ContentStatus status;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private LocalDateTime scheduledPublishAt;

    private Integer priority;
}
