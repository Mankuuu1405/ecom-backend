package com.one.aim.rs;


import com.one.aim.constants.ContentStatus;
import com.one.aim.constants.PageType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InformationalPageRs {
    private Long id;
    private PageType pageType;

    // Computed field - not stored in DB
    private String title; // Set from pageType.getDisplayName()

    private String contentJson;
    private ContentStatus status;
    private String metaDescription;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}