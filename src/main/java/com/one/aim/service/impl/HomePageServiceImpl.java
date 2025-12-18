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
            sections.add(
                    HomeSectionRs.products(
                            "Trending Now",
                            trending,
                            "/products?sort=trending"
                    )
            );
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
            sections.add(
                    HomeSectionRs.products(
                            "New Arrivals",
                            newArrivals,
                            "/products?sort=new"
                    )
            );
        }

        // =========================
// HOT PICKS (FEATURED)
// =========================
        List<ProductCardRs> hotPicks = getHotPicks(limit);

        if (!hotPicks.isEmpty()) {
            sections.add(
                    HomeSectionRs.products(
                            "Hot Picks",
                            hotPicks,
                            "/products?filter=featured"
                    )
            );
        }

        List<ReviewCardRs> review= getTopReviews(limit);

        if (!review.isEmpty()) {
            sections.add(
                    HomeSectionRs.reviews(
                            "Customer Reviews",
                            getTopReviews(3)
                    )
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

        List<Long> productIds =
                orderItemBoRepo.findTopSelling(
                                start,
                                LocalDateTime.now(),
                                PageRequest.of(0, limit)
                        ).stream()
                        .map(r -> ((Number) r[0]).longValue())
                        .distinct()
                        .toList();

        if (productIds.isEmpty()) return Collections.emptyList();

        return productRepo.findAllById(productIds).stream()
                .filter(ProductBO::isActive)
                .map(productMapper::toCardRs)
                .toList();
    }

    // ======================================================
    // NEW ARRIVALS
    // ======================================================
    private List<ProductCardRs> getNewArrivals(int limit) {

        Pageable pageable =
                PageRequest.of(0, limit, Sort.by("createdAt").descending());

        return productRepo.findByActiveTrue(pageable)
                .stream()
                .map(productMapper::toCardRs)
                .toList();
    }

    // ======================================================
    // FEATURED CATEGORIES
    // ======================================================
    private List<CategoryCardRs> getFeaturedCategories(int limit) {

        Pageable pageable = PageRequest.of(0, limit);

        Map<Long, Long> countMap = productRepo.countProductsGrouped()
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));

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
        return productRepo.findByActiveTrueAndFeaturedTrue(pageable)
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

