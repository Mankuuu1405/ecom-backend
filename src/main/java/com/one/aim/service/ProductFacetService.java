package com.one.aim.service;

import com.one.aim.rs.ProductFacetRs;

import java.util.List;

public interface ProductFacetService {

    ProductFacetRs getFacets(
            String q,
            List<Long> categoryIds,
            List<String> brands,
            Integer minPrice,
            Integer maxPrice,
            Integer rating
    );
}

