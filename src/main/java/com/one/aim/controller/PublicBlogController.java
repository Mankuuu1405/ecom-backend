package com.one.aim.controller;

import com.one.aim.constants.ContentStatus;
import com.one.aim.rs.BlogCardRs;
import com.one.aim.rs.PagedRs;
import com.one.aim.service.BlogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/blogs")
@RequiredArgsConstructor
public class PublicBlogController {

    private final BlogService blogService;

    @GetMapping
    public ResponseEntity<PagedRs<BlogCardRs>> getPublishedBlogs(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Sort sort = Sort.by("publishedAt").descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        PagedRs<BlogCardRs> response;
        if (keyword != null && !keyword.trim().isEmpty()) {
            response = blogService.searchBlogs(keyword, ContentStatus.PUBLISHED, pageable);
        } else {
            response = blogService.getBlogsByStatus(ContentStatus.PUBLISHED, pageable);
        }

        return ResponseEntity.ok(response);
    }
    @GetMapping("/{slug}")
    public ResponseEntity<BlogCardRs> getBlogBySlug(@PathVariable String slug) {
        BlogCardRs response = blogService.getBlogBySlug(slug);
        blogService.incrementViewCount(response.getId());
        return ResponseEntity.ok(response);
    }
}