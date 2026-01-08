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
public class BannerRq {
    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    @Size(max = 500, message = "Subtitle must not exceed 500 characters")
    private String subtitle;

    private Long imageFileId;

    @Size(max = 100, message = "Button text must not exceed 100 characters")
    private String buttonText;

    @Size(max = 500, message = "Button link must not exceed 500 characters")
    private String buttonLink;

    private ContentStatus status;

    private LocalDateTime scheduledPublishAt;

    private LocalDateTime scheduledUnpublishAt;

    private Integer priority;

    private String position;

}
