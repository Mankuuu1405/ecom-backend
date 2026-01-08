package com.one.aim.service.impl;

import com.one.aim.repo.ProductRepo;
import com.one.aim.rs.BrandCountRs;
import com.one.aim.rs.PriceRangeRs;
import com.one.aim.rs.ProductFacetRs;
import com.one.aim.service.ProductFacetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductFacetServiceImpl implements ProductFacetService {

    private final ProductRepo productRepository;

    @Override
    public ProductFacetRs getFacets(
            String q,
            List<Long> categoryIds,
            List<String> brands,
            Integer minPrice,
            Integer maxPrice,
            Integer rating
    ) {

        String search = (q == null || q.isBlank()) ? null : q.toLowerCase();

        // Normalize empty lists (CRITICAL)
        if (categoryIds != null && categoryIds.isEmpty()) {
            categoryIds = null;
        }
        if (brands != null && brands.isEmpty()) {
            brands = null;
        }

        // =========================
        // BRAND FACETS
        // Ignore brand filter itself
        // =========================
        List<BrandCountRs> brandFacets =
                productRepository.getBrandFacets(
                        search,
                        categoryIds,
                        minPrice,
                        maxPrice,
                        rating
                );

        // =========================
        // PRICE RANGE FACET
        // Ignore minPrice & maxPrice
        // =========================
        PriceRangeRs priceRange =
                productRepository.getPriceRange(
                        search,
                        categoryIds,
                        brands,
                        rating
                );

        return new ProductFacetRs(brandFacets, priceRange);
    }


}


