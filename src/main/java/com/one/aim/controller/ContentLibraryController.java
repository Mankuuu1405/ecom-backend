package com.one.aim.controller;

import com.one.aim.constants.ContentStatus;
import com.one.aim.constants.ContentType;
import com.one.aim.rs.PagedRs;
import com.one.aim.rs.data.ContentLibraryItem;
import com.one.aim.service.ContentLibraryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/content-library")
@RequiredArgsConstructor
public class ContentLibraryController {

    private final ContentLibraryService contentLibraryService;

    @GetMapping
    public ResponseEntity<PagedRs<ContentLibraryItem>> getAllContent(
            @RequestParam(required = false) ContentType type,
            @RequestParam(required = false) ContentStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "lastUpdated") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("ASC") ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        PagedRs<ContentLibraryItem> response =
                contentLibraryService.getAllContent(type, status, keyword, pageable);

        return ResponseEntity.ok(response);
    }
}
