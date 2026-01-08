package com.one.aim.rs.data;

import com.one.aim.constants.ContentStatus;
import com.one.aim.constants.ContentType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentLibraryItem {
    private Long id;
    private String title;
    private ContentType type;
    private ContentStatus status;
    private LocalDateTime lastUpdated;
}