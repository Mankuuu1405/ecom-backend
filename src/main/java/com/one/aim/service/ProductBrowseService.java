package com.one.aim.service;

import com.one.aim.rs.ProductCardRs;
import org.springframework.data.domain.Page;

public interface ProductBrowseService {
    Page<ProductCardRs> browse(
            String category,
            String brand,
            Integer minPrice,
            Integer maxPrice,
            Integer rating,
            String sort,
            int page,
            int size
    );
}

