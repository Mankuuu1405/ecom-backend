package com.one.aim.service.impl;

import com.one.aim.bo.BannerBO;
import com.one.aim.bo.BlogBO;
import com.one.aim.bo.InformationalPageBO;
import com.one.aim.bo.PromotionBO;
import com.one.aim.constants.ContentStatus;
import com.one.aim.constants.ContentType;
import com.one.aim.repo.BannerRepo;
import com.one.aim.repo.BlogRepo;
import com.one.aim.repo.InformationalPageRepository;
import com.one.aim.repo.PromotionRepository;
import com.one.aim.rs.PagedRs;
import com.one.aim.rs.data.ContentLibraryItem;
import com.one.aim.service.ContentLibraryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContentLibraryServiceImpl implements ContentLibraryService {

    private final BlogRepo blogRepository;
    private final BannerRepo bannerRepository;
    private final InformationalPageRepository pageRepository;
    private final PromotionRepository promotionRepository;

    @Override
    @Transactional(readOnly = true)
    public PagedRs<ContentLibraryItem> getAllContent(
            ContentType type,
            ContentStatus status,
            String keyword,
            Pageable pageable) {

        List<ContentLibraryItem> allContent = new ArrayList<>();

        // Fetch based on type filter
        if (type == null || type == ContentType.BLOG_POST) {
            allContent.addAll(getBlogContent(status, keyword));
        }
        if (type == null || type == ContentType.BANNER) {
            allContent.addAll(getBannerContent(status, keyword));
        }
        if (type == null || type == ContentType.INFORMATIONAL_PAGE) {
            allContent.addAll(getPageContent(status, keyword));
        }
        if (type == null || type == ContentType.PROMOTION) {
            allContent.addAll(getPromotionContent(status, keyword));
        }

        // Sort by last updated (most recent first)
        allContent.sort(Comparator.comparing(ContentLibraryItem::getLastUpdated).reversed());

        // Manual pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), allContent.size());
        List<ContentLibraryItem> pageContent = allContent.subList(start, end);

        return PagedRs.<ContentLibraryItem>builder()
                .content(pageContent)
                .pageNumber(pageable.getPageNumber())
                .pageSize(pageable.getPageSize())
                .totalElements(allContent.size())
                .totalPages((int) Math.ceil((double) allContent.size() / pageable.getPageSize()))
                .last(end >= allContent.size())
                .first(start == 0)
                .build();
    }

    private List<ContentLibraryItem> getBlogContent(ContentStatus status, String keyword) {
        List<BlogBO> blogs = blogRepository.findAll();
        return blogs.stream()
                .filter(blog -> status == null || blog.getStatus() == status)
                .filter(blog -> keyword == null || keyword.trim().isEmpty() ||
                        blog.getTitle().toLowerCase().contains(keyword.toLowerCase()))
                .map(blog -> ContentLibraryItem.builder()
                        .id(blog.getId())
                        .title(blog.getTitle())
                        .type(ContentType.BLOG_POST)
                        .status(blog.getStatus())
                        .lastUpdated(blog.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    private List<ContentLibraryItem> getBannerContent(ContentStatus status, String keyword) {
        List<BannerBO> banners = bannerRepository.findAll();
        return banners.stream()
                .filter(banner -> status == null || banner.getStatus() == status)
                .filter(banner -> keyword == null || keyword.trim().isEmpty() ||
                        banner.getTitle().toLowerCase().contains(keyword.toLowerCase()))
                .map(banner -> ContentLibraryItem.builder()
                        .id(banner.getId())
                        .title(banner.getTitle())
                        .type(ContentType.BANNER)
                        .status(banner.getStatus())
                        .lastUpdated(banner.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    private List<ContentLibraryItem> getPageContent(ContentStatus status, String keyword) {
        List<InformationalPageBO> pages = pageRepository.findAll();
        return pages.stream()
                .filter(page -> status == null || page.getStatus() == status)
                .filter(page -> keyword == null || keyword.trim().isEmpty() ||
                        page.getTitle().toLowerCase().contains(keyword.toLowerCase()))
                .map(page -> ContentLibraryItem.builder()
                        .id(page.getId())
                        .title(page.getTitle())
                        .type(ContentType.INFORMATIONAL_PAGE)
                        .status(page.getStatus())
                        .lastUpdated(page.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    private List<ContentLibraryItem> getPromotionContent(ContentStatus status, String keyword) {
        List<PromotionBO> promotions = promotionRepository.findAll();
        return promotions.stream()
                .filter(promotion -> status == null || promotion.getStatus() == status)
                .filter(promotion -> keyword == null || keyword.trim().isEmpty() ||
                        promotion.getTitle().toLowerCase().contains(keyword.toLowerCase()))
                .map(promotion -> ContentLibraryItem.builder()
                        .id(promotion.getId())
                        .title(promotion.getTitle())
                        .type(ContentType.PROMOTION)
                        .status(promotion.getStatus())
                        .lastUpdated(promotion.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());
    }
}

