package com.one.aim.controller;

import com.one.aim.bo.InformationalPageBO;
import com.one.aim.constants.PageType;
import com.one.aim.mapper.InformationalPageMapper;
import com.one.aim.repo.InformationalPageRepository;
import com.one.aim.rs.InformationalPageRs;
import com.one.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

@RestController
@RequestMapping("/api/v1/public/pages")
@RequiredArgsConstructor
public class InformationalPagePublicController {

    private final InformationalPageRepository pageRepository;
    private final InformationalPageMapper pageMapper;

    /**
     * Get page by PageType (e.g., /faq, /about)
     */
    @GetMapping("/type/{pageType}")
    public InformationalPageRs getPageByType(@PathVariable PageType pageType) {
        InformationalPageBO page = pageRepository.findByPageType(pageType)
                .orElseThrow(() -> new ResourceNotFoundException("Page not found: " + pageType));

        InformationalPageRs rs = pageMapper.toResponseDTO(page);
        rs.setTitle(pageType.getDisplayName()); // auto set title based on enum
        return rs;
    }

    /**
     * Get page by frontend route path (e.g., /faq, /privacy)
     */
    @GetMapping("/route")
    public InformationalPageRs getPageByRoute(@RequestParam String path) {
        PageType pageType = Arrays.stream(PageType.values())
                .filter(pt -> pt.getRoutePath().equalsIgnoreCase("/" + path))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Invalid route: " + path));

        return getPageByType(pageType);
    }
}

