package com.one.aim.service.impl;

import com.one.aim.mapper.ProductMapper;
import com.one.aim.repo.ProductRepo;
import com.one.aim.rs.*;
import com.one.aim.service.PdpService;
import com.one.aim.service.ProductService;
import com.one.aim.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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

    @Override
    public PdpRs getPdpBySlug(String slug) throws Exception {

        ProductDetailsRs product =
                productService.getProductDetails(slug);

        Page<ReviewRs> reviews =
                reviewService.getReviewsByProductSlug(slug, 0, 10);

        //  People Also Bought (ranked)
        List<ProductCardRs> peopleAlsoBought =
                recommendationService.getPeopleAlsoBought(
                        product.getId(),
                        product.getCategoryName(),
                        6
                );

        //  Frequently Bought Together
        List<ProductCardRs> frequentlyBoughtTogether =
                recommendationService.getFrequentlyBoughtTogether(
                        product.getId(),
                        4
                );

        //  Similar products (same category, exclude self)
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

        //  Remove duplicates already shown
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
                        .filter(p ->
                                !alreadyShown.contains(
                                        Long.valueOf(p.getDocId())
                                )
                        )
                        .limit(6)
                        .toList();

        //  Build recommendation block
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

