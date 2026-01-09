package com.one.aim.rq;

import com.one.aim.constants.ContentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class BlogCreateRq {
    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    @NotBlank(message = "Content is required")
    private String content;


    private String slug;

    private Long imageFileId;

    private ContentStatus status;

    private LocalDateTime scheduledPublishAt;

    @Size(max = 500, message = "Meta description must not exceed 500 characters")
    private String metaDescription;

    @Size(max = 500, message = "Keywords must not exceed 500 characters")
    private String keywords;

    private String author;
}