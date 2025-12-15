package com.one.aim.service;

import com.one.aim.rs.ProductCardRs;
import com.one.aim.rs.RecommendationRs;

import java.util.List;

public interface RecommendationService {

    // For homepage/listing pages - returns lightweight RecommendationRs
    List<RecommendationRs> getFrequentlyBoughtTogetherSimple(Long productId, int limit);
    List<RecommendationRs> getPeopleAlsoBoughtSimple(Long productId, int limit);
    List<RecommendationRs> getRecommendedForUserSimple(int limit);
    List<RecommendationRs> getTrendingSimple(int days, int limit);
    List<RecommendationRs> getTopByCategorySimple(String categoryOrSlug, int limit);

    // For product detail page - returns full ProductCardRs
    List<ProductCardRs> getFrequentlyBoughtTogether(Long productId, int limit);
    public List<ProductCardRs> getPeopleAlsoBought(
            Long productId,
            String categoryName,
            int limit
    );

    List<ProductCardRs> getRecommendedForUser(int limit);
    List<ProductCardRs> getTrending(int days, int limit);
    List<ProductCardRs> getTopByCategory(String categoryOrSlug, int limit);
}