package com.one.aim.controller;

import com.one.aim.constants.ContentStatus;
import com.one.aim.rq.BlogCreateRq;
import com.one.aim.rs.BlogCardRs;
import com.one.aim.rs.PagedRs;
import com.one.aim.service.BlogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/blogs")
@RequiredArgsConstructor
public class BlogController {


    private final BlogService blogService;

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<BlogCardRs> createBlog(
            @RequestPart("blog") @Valid BlogCreateRq rq,
            @RequestPart(value = "image", required = false) MultipartFile image) throws Exception {

        BlogCardRs response = blogService.createBlog(rq, image);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    public ResponseEntity<BlogCardRs> updateBlog(
            @PathVariable Long id,
            @RequestPart("blog") @Valid BlogCreateRq rq,
            @RequestPart(value = "image", required = false) MultipartFile image) throws Exception {

        BlogCardRs response = blogService.updateBlog(id, rq, image);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BlogCardRs> getBlogById(@PathVariable Long id) {
        BlogCardRs response = blogService.getBlogById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<BlogCardRs> getBlogBySlug(@PathVariable String slug) {
        BlogCardRs response = blogService.getBlogBySlug(slug);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<PagedRs<BlogCardRs>> getAllBlogs(
            @RequestParam(required = false) ContentStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("ASC") ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        PagedRs<BlogCardRs> response;
        if (keyword != null && !keyword.trim().isEmpty()) {
            response = blogService.searchBlogs(keyword, status, pageable);
        } else if (status != null) {
            response = blogService.getBlogsByStatus(status, pageable);
        } else {
            response = blogService.getAllBlogs(pageable);
        }

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBlog(@PathVariable Long id) {
        blogService.deleteBlog(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/increment-view")
    public ResponseEntity<Void> incrementViewCount(@PathVariable Long id) {
        blogService.incrementViewCount(id);
        return ResponseEntity.ok().build();
    }
}
