package com.one.aim.service.impl;

import com.one.aim.bo.ProductBO;
import com.one.aim.repo.ProductRepo;
import com.one.aim.repo.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductRatingService {

    private final ProductRepo productRepo;
    private final ReviewRepository reviewRepo;

    @Transactional
    public void updateProductRating(Long productId) {

        ProductBO product = productRepo.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        Double avg = reviewRepo.getAverageRatingByProductId(productId);
        Long count = reviewRepo.getReviewCountByProductId(productId);

        product.setAverageRating(avg != null ? avg : 0.0);
        product.setReviewCount(count != null ? count : 0L);

        productRepo.save(product);
    }
}

