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

        ProductDetailsRs product = productService.getProductDetails(slug);
        Long productId = product.getId();

    /* ===========================
       RATING SUMMARY
    ============================ */

        Double avgRating = reviewRepo.getAverageRatingByProductId(productId);
        Long reviewCount = reviewRepo.getReviewCountByProductId(productId);

        List<Object[]> rawDistribution =
                reviewRepo.getRatingDistributionByProductId(productId);

        Map<Integer, Long> ratingMap = new LinkedHashMap<>();
        for (int i = 5; i >= 1; i--) ratingMap.put(i, 0L);

        for (Object[] row : rawDistribution) {
            ratingMap.put((Integer) row[0], (Long) row[1]);
        }

        long total = reviewCount != null ? reviewCount : 0L;

        List<RatingDistributionRs> distribution =
                ratingMap.entrySet().stream()
                        .map(e -> new RatingDistributionRs(
                                e.getKey(),
                                e.getValue().intValue(),
                                total == 0 ? 0 : (int) Math.round(e.getValue() * 100.0 / total)
                        ))
                        .toList();

        product.setAverageRating(avgRating != null ? avgRating : 0.0);
        product.setReviewCount(total);
        product.setRatingDistribution(distribution);

    /* ===========================
       RECOMMENDATIONS (UNCHANGED)
    ============================ */

        RecommendationBlockRs recos =
                RecommendationBlockRs.builder()
                        .peopleAlsoBought(
                                recommendationService.getPeopleAlsoBought(
                                        productId,
                                        product.getCategoryName(),
                                        6
                                )
                        )
                        .frequentlyBoughtTogether(
                                recommendationService.getFrequentlyBoughtTogether(
                                        productId,
                                        4
                                )
                        )
                        .similarProducts(
                                productRepo
                                        .findByActiveTrueAndCategoryNameIgnoreCaseAndIdNot(
                                                product.getCategoryName(),
                                                productId,
                                                PageRequest.of(0, 10)
                                        )
                                        .stream()
                                        .map(productMapper::toCardRs)
                                        .limit(6)
                                        .toList()
                        )
                        .build();

        return PdpRs.builder()
                .product(product)
                .recommendations(recos)
                .build();
    }

}
