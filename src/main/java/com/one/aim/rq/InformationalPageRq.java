package com.one.aim.rq;

import com.one.aim.constants.ContentStatus;
import com.one.aim.constants.PageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InformationalPageRq {
    @NotNull(message = "Page type is required")
    private PageType pageType;

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    @NotBlank(message = "Content is required")
    private String content;

    private ContentStatus status;

    @Size(max = 500, message = "Meta description must not exceed 500 characters")
    private String metaDescription;
}
