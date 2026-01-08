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
    private String title;
    private String content;
    private ContentStatus status;
    private String metaDescription;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
