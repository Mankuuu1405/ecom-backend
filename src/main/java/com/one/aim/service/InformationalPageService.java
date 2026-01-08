package com.one.aim.service;

import com.one.aim.constants.PageType;
import com.one.aim.rq.InformationalPageRq;
import com.one.aim.rs.InformationalPageRs;
import com.one.aim.rs.PagedRs;
import org.springframework.data.domain.Pageable;

public interface InformationalPageService {
    InformationalPageRs createOrUpdatePage(InformationalPageRq requestDTO);

    InformationalPageRs getPageById(Long id);

    InformationalPageRs getPageByType(PageType pageType);

    PagedRs<InformationalPageRs> getAllPages(Pageable pageable);

    void deletePage(Long id);
}
