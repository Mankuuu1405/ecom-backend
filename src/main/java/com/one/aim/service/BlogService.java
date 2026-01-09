package com.one.aim.service;

import com.one.aim.constants.ContentStatus;
import com.one.aim.rq.BlogCreateRq;
import com.one.aim.rs.BlogCardRs;
import com.one.aim.rs.PagedRs;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface BlogService {

    BlogCardRs createBlog(BlogCreateRq rq, MultipartFile image) throws Exception;
    BlogCardRs updateBlog(Long id, BlogCreateRq rq, MultipartFile image) throws Exception;

    BlogCardRs getBlogById(Long id);

    BlogCardRs getBlogBySlug(String slug);

    PagedRs<BlogCardRs> getAllBlogs(Pageable pageable);

    PagedRs<BlogCardRs> getBlogsByStatus(ContentStatus status, Pageable pageable);

    PagedRs<BlogCardRs> searchBlogs(String keyword, ContentStatus status, Pageable pageable);

    void deleteBlog(Long id);

    void publishScheduledBlogs();

    void incrementViewCount(Long id);
}