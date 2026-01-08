package com.one.aim.service;

import com.one.aim.constants.ContentStatus;
import com.one.aim.constants.ContentType;
import com.one.aim.rs.PagedRs;
import com.one.aim.rs.data.ContentLibraryItem;
import org.springframework.data.domain.Pageable;

public interface ContentLibraryService {

    PagedRs<ContentLibraryItem> getAllContent(
            ContentType type,
            ContentStatus status,
            String keyword,
            Pageable pageable
    );
}
