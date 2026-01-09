package com.one.aim.service.impl;

import com.one.aim.bo.BannerBO;
import com.one.aim.bo.ProductBO;
import com.one.aim.bo.PromotionBO;
import com.one.aim.constants.ContentStatus;
import com.one.aim.mapper.CategoryMapper;
import com.one.aim.mapper.ProductMapper;
import com.one.aim.repo.*;
import com.one.aim.rs.*;
import com.one.aim.service.HomePageService;

import com.one.aim.service.ServiceModuleService;
import com.one.utils.UrlUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomePageServiceImpl implements HomePageService {

    private final ProductRepo productRepo;
    private final CategoryRepo categoryRepo;
    private final BannerRepo bannerRepo;
    private final ServiceModuleService serviceModuleService;
    private final ProductMapper productMapper;
    private final OrderItemBORepo orderItemBoRepo;
    private final CategoryMapper categoryMapper;
    private final UrlUtils urlUtils;
    private final ReviewRepository reviewRepo;
    private final BlogRepo blogRepo;
    private final PromotionRepository promotionRepo;

    @Override
    public HomePageRs getHomePage(int limit) {

        List<HomeSectionRs> sections = new ArrayList<>();
        Set<Long> shownProductIds = new HashSet<>();

        // =========================
        // TRENDING PRODUCTS
        // =========================
        List<ProductCardRs> trending = getTrendingProducts(limit);
        trending.forEach(p -> shownProductIds.add(Long.valueOf(p.getDocId())));

        if (!trending.isEmpty()) {
            sections.add(HomeSectionRs.products("Trending Now", getTrendingProducts(limit), "/products?sort=trending"));
        }

        // =========================
        // NEW ARRIVALS (EXCLUDE TRENDING)
        // =========================
        List<ProductCardRs> newArrivals =
                getNewArrivals(limit * 2).stream()
                        .filter(p -> !shownProductIds.contains(Long.valueOf(p.getDocId())))
                        .limit(limit)
                        .toList();

        if (!newArrivals.isEmpty()) {
            sections.add(HomeSectionRs.products("New Arrivals", getNewArrivals(limit), "/products?sort=new"));
        }

        // =========================
// HOT PICKS (FEATURED)
// =========================
        List<ProductCardRs> hotPicks = getHotPicks(limit);

        if (!hotPicks.isEmpty()) {
            sections.add(HomeSectionRs.products("Hot Picks", getHotPicks(limit), "/products?filter=featured"));
        }

        List<ReviewCardRs> reviews = getTopReviews(3);

        if (!reviews.isEmpty()) {
            sections.add(
                    HomeSectionRs.reviews("Customer Reviews", reviews)
            );
        }

        List<PromotionRs> promotions = getActivePromotions(2);

        if (!promotions.isEmpty()) {
            sections.add(HomeSectionRs.promotions(
                    "Special Offers",
                    promotions
            ));
        }

        // =========================
// BLOGS
// =========================
        List<BlogCardRs> blogs = getPublishedBlogs(3);

        if (!blogs.isEmpty()) {
            sections.add(HomeSectionRs.blogs(
                    "From Our Blog",
                    blogs,
                    "/blog"
            ));
        }



        // =========================
        // TOP SERVICES
        // =========================
        List<ServiceCardRs> services = serviceModuleService.getTopServices(4);

        if (!services.isEmpty()) {
            sections.add(
                    HomeSectionRs.services(
                            "Top Services",
                            services
                    )
            );
        }


        // =========================
        // CATEGORIES
        // =========================
        List<CategoryCardRs> categories = getFeaturedCategories(6);
        if (!categories.isEmpty()) {
            sections.add(
                    HomeSectionRs.categories("Shop by Category", categories)
            );
        }

        // =========================
        // FINAL RESPONSE
        // =========================
        return HomePageRs.builder()
                .banners(getActiveBanners())
                .sections(sections)
                .build();
    }

    // ======================================================
    // TRENDING PRODUCTS (NO N+1 QUERY)
    // ======================================================
    private List<ProductCardRs> getTrendingProducts(int limit) {

        LocalDateTime start = LocalDateTime.now().minusDays(30);

        Pageable pageable = PageRequest.of(0, limit);

        return productRepo.findTrendingProducts(
                        start,
                        LocalDateTime.now(),
                        pageable
                )
                .stream()
                .map(productMapper::toCardRs)
                .toList();
    }

    // ======================================================
    // NEW ARRIVALS
    // ======================================================
    private List<ProductCardRs> getNewArrivals(int limit) {

        Pageable pageable = PageRequest.of(0, limit);

        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);

        return productRepo.findHomeNewArrivals(cutoff, pageable)
                .stream()
                .map(productMapper::toCardRs)
                .toList();
    }


    // ======================================================
    // FEATURED CATEGORIES
    // ======================================================
    private List<CategoryCardRs> getFeaturedCategories(int limit) {

        Pageable pageable = PageRequest.of(0, limit);

        Map<Long, Long> countMap = productRepo.countProductsGroupedByCategoryAsMap();

        return categoryRepo.findTopActiveCategories(pageable)
                .stream()
                .map(category ->
                        categoryMapper.toBrowseRs(
                                category,
                                countMap.getOrDefault(category.getId(), 0L)
                        )
                )
                .toList();
    }

    // ======================================================
    // BANNERS (ABSOLUTE IMAGE URL)
    // ======================================================
    private List<BannerRs> getActiveBanners() {
        LocalDateTime now = LocalDateTime.now();

        // Get all PUBLISHED banners that are currently active
        List<BannerBO> activeBanners = bannerRepo.findActiveBanners(
                ContentStatus.PUBLISHED,
                now
        );

        return activeBanners.stream()
                .sorted(Comparator.comparing(BannerBO::getPriority).reversed())
                .map(banner -> BannerRs.builder()
                        .id(banner.getId())
                        .title(banner.getTitle())
                        .subtitle(banner.getSubtitle())
                        .imageUrl(banner.getImageFileId() != null
                                ? urlUtils.publicFile(banner.getImageFileId())
                                : urlUtils.defaultBanner())
                        .buttonText(banner.getButtonText())
                        .buttonLink(banner.getButtonLink())
                        .priority(banner.getPriority())
                        .position(banner.getPosition())
                        .build())
                .toList();
    }

    private List<ProductCardRs> getHotPicks(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return productRepo
                .findByActiveTrueAndFeaturedTrueOrderByUpdatedAtDesc(pageable)
                .stream()
                .map(productMapper::toCardRs)
                .toList();
    }


    private List<ReviewCardRs> getTopReviews(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return reviewRepo.findTopReviews(pageable)
                .stream()
                .map(r -> new ReviewCardRs(
                        r.getUser().getFullName(),
                        r.getRating(),
                        r.getComment()
                ))
                .toList();
    }


    private List<BlogCardRs> getPublishedBlogs(int limit) {
        Pageable pageable = PageRequest.of(0, limit);

        // Get only PUBLISHED blogs, ordered by publish date
        return blogRepo.findByStatus(ContentStatus.PUBLISHED, pageable)
                .getContent()
                .stream()
                .map(blog -> BlogCardRs.builder()
                        .id(blog.getId())
                        .title(blog.getTitle())
                        .slug(blog.getSlug())
                        .imageUrl(blog.getImageFileId() != null
                                ? urlUtils.publicFile(blog.getImageFileId())
                                : urlUtils.defaultImage())
                        .metaDescription(blog.getMetaDescription())
                        .author(blog.getAuthor())
                        .publishedAt(blog.getPublishedAt())
                        .viewCount(blog.getViewCount())
                        .build())
                .toList();
    }

    private List<PromotionRs> getActivePromotions(int limit) {
        LocalDateTime now = LocalDateTime.now();

        // Get all active promotions (PUBLISHED status, within date range)
        List<PromotionBO> activePromotions = promotionRepo.findActivePromotions(
                ContentStatus.PUBLISHED,
                now
        );

        return activePromotions.stream()
                .sorted(Comparator.comparing(PromotionBO::getPriority).reversed())
                .limit(limit)
                .map(promo -> PromotionRs.builder()
                        .id(promo.getId())
                        .title(promo.getTitle())
                        .description(promo.getDescription())
                        .imageUrl(promo.getImageFileId() != null
                                ? urlUtils.publicFile(promo.getImageFileId())
                                : urlUtils.defaultImage())
                        .discountPercentage(promo.getDiscountPercentage())
                        .discountCode(promo.getDiscountCode())
                        .startDate(promo.getStartDate())
                        .endDate(promo.getEndDate())
                        .build())
                .toList();
    }

    private String getTimeAgo(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "recently";
        }

        LocalDateTime now = LocalDateTime.now();
        long days = java.time.Duration.between(dateTime, now).toDays();

        if (days == 0) {
            long hours = java.time.Duration.between(dateTime, now).toHours();
            if (hours == 0) {
                long minutes = java.time.Duration.between(dateTime, now).toMinutes();
                return minutes + " minute" + (minutes != 1 ? "s" : "") + " ago";
            }
            return hours + " hour" + (hours != 1 ? "s" : "") + " ago";
        } else if (days < 7) {
            return days + " day" + (days != 1 ? "s" : "") + " ago";
        } else if (days < 30) {
            long weeks = days / 7;
            return weeks + " week" + (weeks != 1 ? "s" : "") + " ago";
        } else if (days < 365) {
            long months = days / 30;
            return months + " month" + (months != 1 ? "s" : "") + " ago";
        } else {
            long years = days / 365;
            return years + " year" + (years != 1 ? "s" : "") + " ago";
        }
    }
}