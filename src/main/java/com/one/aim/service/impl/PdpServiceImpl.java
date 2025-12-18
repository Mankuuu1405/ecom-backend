package com.one.aim.service.impl;

import com.one.aim.mapper.ProductMapper;
import com.one.aim.repo.ProductRepo;
import com.one.aim.repo.ReviewRepository;
import com.one.aim.rs.*;
import com.one.aim.service.PdpService;
import com.one.aim.service.ProductService;
import com.one.aim.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PdpServiceImpl implements PdpService {

    private final ProductService productService;
    private final ReviewService reviewService;
    private final RecommendationService recommendationService;
    private final ProductRepo productRepo;
    private final ProductMapper productMapper;
    private final ReviewRepository reviewRepo;

    @Override
    public PdpRs getPdpBySlug(String slug) throws Exception {

        ProductDetailsRs product =
                productService.getProductDetails(slug);

        Page<ReviewRs> reviews =
                reviewService.getReviewsByProductSlug(slug, 0, 10);

    /* ======================================================
       ⭐ RATING SUMMARY (IMPORTANT FIX)
    ====================================================== */

        Long productId = product.getId();

        // Average rating
        Double avgRating =
                reviewRepo.getAverageRatingByProductId(productId);

        // Total reviews
        Long reviewCount =
                reviewRepo.getReviewCountByProductId(productId);

        // Rating distribution (1–5)
        List<Object[]> rawDistribution =
                reviewRepo.getRatingDistributionByProductId(productId);

        // Initialize all stars with 0
        Map<Integer, Long> ratingMap = new LinkedHashMap<>();
        for (int i = 5; i >= 1; i--) {
            ratingMap.put(i, 0L);
        }

        // Fill actual counts
        for (Object[] row : rawDistribution) {
            Integer rating = (Integer) row[0];
            Long count = (Long) row[1];
            ratingMap.put(rating, count);
        }

        Long totalReviews = reviewCount != null ? reviewCount : 0L;

        List<RatingDistributionRs> distribution =
                ratingMap.entrySet().stream()
                        .map(e -> {
                            int stars = e.getKey();
                            long count = e.getValue();

                            int percentage =
                                    totalReviews == 0
                                            ? 0
                                            : (int) Math.round((count * 100.0) / totalReviews);

                            return new RatingDistributionRs(
                                    stars,
                                    (int) count,
                                    percentage
                            );
                        })
                        .toList();


        // Set into product
        product.setAverageRating(avgRating != null ? avgRating : 0.0);
        product.setReviewCount(reviewCount != null ? reviewCount : 0);
        product.setRatingDistribution(distribution);

    /* ======================================================
       RECOMMENDATIONS (UNCHANGED)
    ====================================================== */

        List<ProductCardRs> peopleAlsoBought =
                recommendationService.getPeopleAlsoBought(
                        product.getId(),
                        product.getCategoryName(),
                        6
                );

        List<ProductCardRs> frequentlyBoughtTogether =
                recommendationService.getFrequentlyBoughtTogether(
                        product.getId(),
                        4
                );

        List<ProductCardRs> similarProducts =
                productRepo
                        .findByActiveTrueAndCategoryNameIgnoreCaseAndIdNot(
                                product.getCategoryName(),
                                product.getId(),
                                PageRequest.of(0, 10)
                        )
                        .getContent()
                        .stream()
                        .map(productMapper::toCardRs)
                        .toList();

        Set<Long> alreadyShown =
                Stream.concat(
                                peopleAlsoBought.stream(),
                                frequentlyBoughtTogether.stream()
                        )
                        .map(ProductCardRs::getDocId)
                        .map(Long::valueOf)
                        .collect(Collectors.toSet());

        similarProducts =
                similarProducts.stream()
                        .filter(p -> !alreadyShown.contains(Long.valueOf(p.getDocId())))
                        .limit(6)
                        .toList();

        RecommendationBlockRs recos =
                RecommendationBlockRs.builder()
                        .peopleAlsoBought(peopleAlsoBought)
                        .frequentlyBoughtTogether(frequentlyBoughtTogether)
                        .similarProducts(similarProducts)
                        .build();

        return PdpRs.builder()
                .product(product)
                .reviews(reviews)
                .recommendations(recos)
                .build();
    }
}
