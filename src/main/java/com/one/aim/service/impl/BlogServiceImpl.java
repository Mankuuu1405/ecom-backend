package com.one.aim.service.impl;

import com.one.aim.bo.BlogBO;
import com.one.aim.bo.FileBO;
import com.one.aim.constants.ContentStatus;
import com.one.aim.mapper.BlogMapper;
import com.one.aim.repo.BlogRepo;
import com.one.aim.rq.BlogCreateRq;
import com.one.aim.rs.BlogCardRs;
import com.one.aim.rs.PagedRs;
import com.one.aim.rs.data.FileDataRs;
import com.one.aim.service.BlogService;
import com.one.aim.service.FileService;
import com.one.exception.ResourceNotFoundException;
import com.one.utils.UrlUtils;
import com.one.vm.core.BaseRs;
import com.one.vm.utils.SlugUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BlogServiceImpl implements BlogService {

    private final BlogRepo blogRepository;
    private final BlogMapper blogMapper;
    private final FileService fileService;
    private final UrlUtils urlUtils;

    @Override
    @Transactional
    public BlogCardRs createBlog(BlogCreateRq rq, MultipartFile image) throws Exception {
        // Handle image upload if provided
        if (image != null && !image.isEmpty()) {
            FileBO uploadedFile = fileService.uploadAndReturnFile(image);
            rq.setImageFileId(uploadedFile.getId());
        }

        String slug = generateUniqueSlug(rq.getTitle());
        rq.setSlug(slug);

        BlogBO blog = blogMapper.toEntity(rq);

        if (blog.getStatus() == ContentStatus.PUBLISHED) {
            blog.setPublishedAt(LocalDateTime.now());
        }

        BlogBO savedBlog = blogRepository.save(blog);
        log.info("Blog created with ID: {}", savedBlog.getId());
        return toResponseWithImageUrl(savedBlog);
    }

    @Override
    @Transactional
    public BlogCardRs updateBlog(Long id, BlogCreateRq rq, MultipartFile image) throws Exception {
        BlogBO blog = blogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blog not found with ID: " + id));

        // Handle new image upload if provided
        if (image != null && !image.isEmpty()) {
            // Delete old image if exists
            if (blog.getImageFileId() != null) {
                try {
                    fileService.deleteFile(blog.getImageFileId());
                } catch (Exception e) {
                    log.warn("Failed to delete old blog image: {}", e.getMessage());
                }
            }

            // Upload new image
            FileBO uploadedFile = fileService.uploadAndReturnFile(image);
            rq.setImageFileId(uploadedFile.getId());
        }

        // REGENERATE SLUG IF TITLE CHANGED
        if (!blog.getTitle().equals(rq.getTitle())) {
            String newSlug = generateUniqueSlug(rq.getTitle(), id);
            rq.setSlug(newSlug);
            log.info("Blog slug updated from '{}' to '{}'", blog.getSlug(), newSlug);
        } else {
            // Keep existing slug
            rq.setSlug(blog.getSlug());
        }

        ContentStatus oldStatus = blog.getStatus();
        blogMapper.updateEntityFromDTO(rq, blog);

        if (oldStatus != ContentStatus.PUBLISHED && blog.getStatus() == ContentStatus.PUBLISHED) {
            blog.setPublishedAt(LocalDateTime.now());
        }

        BlogBO updatedBlog = blogRepository.save(blog);
        log.info("Blog updated with ID: {}", updatedBlog.getId());
        return toResponseWithImageUrl(updatedBlog);
    }


    @Override
    @Transactional(readOnly = true)
    public BlogCardRs getBlogById(Long id) {
        BlogBO blog = blogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blog not found with ID: " + id));
        return toResponseWithImageUrl(blog);
    }

    @Override
    @Transactional(readOnly = true)
    public BlogCardRs getBlogBySlug(String slug) {
        BlogBO blog = blogRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Blog not found with slug: " + slug));
        return toResponseWithImageUrl(blog);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedRs<BlogCardRs> getAllBlogs(Pageable pageable) {
        Page<BlogBO> blogPage = blogRepository.findAll(pageable);
        return buildPagedResponse(blogPage);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedRs<BlogCardRs> getBlogsByStatus(ContentStatus status, Pageable pageable) {
        Page<BlogBO> blogPage = blogRepository.findByStatus(status, pageable);
        return buildPagedResponse(blogPage);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedRs<BlogCardRs> searchBlogs(String keyword, ContentStatus status, Pageable pageable) {
        Specification<BlogBO> spec = Specification.where(null);

        if (keyword != null && !keyword.trim().isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.or(
                            cb.like(cb.lower(root.get("title")), "%" + keyword.toLowerCase() + "%"),
                            cb.like(cb.lower(root.get("content")), "%" + keyword.toLowerCase() + "%"),
                            cb.like(cb.lower(root.get("keywords")), "%" + keyword.toLowerCase() + "%")
                    )
            );
        }

        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }

        Page<BlogBO> blogPage = blogRepository.findAll(spec, pageable);
        return buildPagedResponse(blogPage);
    }

    @Override
    @Transactional
    public void deleteBlog(Long id) {
        BlogBO blog = blogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blog not found with ID: " + id));

        // Delete associated image if exists
        if (blog.getImageFileId() != null) {
            try {
                fileService.deleteFile(blog.getImageFileId());
            } catch (Exception e) {
                log.warn("Failed to delete blog image: {}", e.getMessage());
            }
        }

        blogRepository.deleteById(id);
        log.info("Blog deleted with ID: {}", id);
    }

    @Override
    @Transactional
    public void publishScheduledBlogs() {
        List<BlogBO> scheduledBlogs = blogRepository.findScheduledBlogs(
                ContentStatus.SCHEDULED,
                LocalDateTime.now()
        );

        scheduledBlogs.forEach(blog -> {
            blog.setStatus(ContentStatus.PUBLISHED);
            blog.setPublishedAt(LocalDateTime.now());
        });

        blogRepository.saveAll(scheduledBlogs);
        log.info("Published {} scheduled blogs", scheduledBlogs.size());
    }

    @Override
    @Transactional
    public void incrementViewCount(Long id) {
        BlogBO blog = blogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blog not found with ID: " + id));
        blog.setViewCount(blog.getViewCount() + 1);
        blogRepository.save(blog);
    }



    private BlogCardRs toResponseWithImageUrl(BlogBO blog) {
        BlogCardRs rs = blogMapper.toResponseDTO(blog);
        if (blog.getImageFileId() != null) {
            rs.setImageUrl(urlUtils.publicFile(blog.getImageFileId()));
        } else {
            rs.setImageUrl(urlUtils.defaultImage());
        }
        return rs;
    }

    private PagedRs<BlogCardRs> buildPagedResponse(Page<BlogBO> page) {
        List<BlogCardRs> content = page.getContent().stream()
                .map(this::toResponseWithImageUrl)
                .collect(Collectors.toList());

        return PagedRs.<BlogCardRs>builder()
                .content(content)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .first(page.isFirst())
                .build();
    }

    private String generateUniqueSlug(String title) {
        return generateUniqueSlug(title, null);
    }

    /**
     * Generate a unique slug from title
     * @param title The blog title
     * @param excludeId Blog ID to exclude from uniqueness check (for updates)
     * @return Unique slug
     */
    private String generateUniqueSlug(String title, Long excludeId) {
        String baseSlug = SlugUtils.from(title);

        if (baseSlug == null || baseSlug.isEmpty()) {
            baseSlug = "blog-post";
        }

        String slug = baseSlug;
        int counter = 1;

        // Keep trying until we find a unique slug
        while (slugExists(slug, excludeId)) {
            slug = baseSlug + "-" + counter;
            counter++;
        }

        return slug;
    }

    /**
     * Check if slug already exists
     * @param slug The slug to check
     * @param excludeId Blog ID to exclude from check (null for new blogs)
     * @return true if slug exists
     */
    private boolean slugExists(String slug, Long excludeId) {
        return blogRepository.findBySlug(slug)
                .map(blog -> excludeId == null || !blog.getId().equals(excludeId))
                .orElse(false);
    }

}