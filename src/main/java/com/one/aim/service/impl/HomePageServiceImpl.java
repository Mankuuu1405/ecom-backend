package com.one.aim.service.impl;

import com.one.aim.bo.ProductBO;
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


        // =========================
// BLOGS
// =========================
        List<BlogCardRs> blogs = getBlogs(3);

        if (!blogs.isEmpty()) {
            sections.add(
                    HomeSectionRs.blogs(
                            "From Our Blog",
                            blogs
                    )
            );
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
                .banners(getBanners())
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
    private List<BannerRs> getBanners() {

        return bannerRepo.findActiveBanners()
                .stream()
                .map(b -> new BannerRs(
                        b.getId(),
                        b.getTitle(),
                        b.getSubtitle(),
                        b.getImageFileId() != null
                                ? urlUtils.publicFile(b.getImageFileId())
                                : urlUtils.defaultBanner(),
                        b.getButtonText(),
                        b.getButtonLink()
                ))
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


    private List<BlogCardRs> getBlogs(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return blogRepo.findByActiveTrueOrderByCreatedAtDesc(pageable)
                .stream()
                .map(b -> new BlogCardRs(
                        b.getTitle(),
                        b.getSlug(),
                        urlUtils.publicFile(b.getImageFileId())
                ))
                .toList();
    }



}

