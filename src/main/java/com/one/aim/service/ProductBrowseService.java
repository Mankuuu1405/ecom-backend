package com.one.aim.service;

import com.one.aim.rs.ProductCardRs;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ProductBrowseService {

    /**
     * Unified search API
     * Used by:
     * - Search page
     * - Shop / All products page
     * - Multi-filter UI
     */
    Page<ProductCardRs> search(
            String q,                     // product / category / brand text
            List<Long> categoryIds,        // MULTI category filter
            String gender,
            List<String> brands,           // MULTI brand filter
            Integer minPrice,
            Integer maxPrice,
            Integer rating,
            String sort,
            int page,
            int size
    );

    /**
     * Backward-compatible browse API (OLD callers only)
     * Internally delegates to search()
     */
    Page<ProductCardRs> browse(
            String categorySlug,           // single category (OLD)
            String brand,                  // single brand (OLD)
            Integer minPrice,
            Integer maxPrice,
            Integer rating,
            String sort,
            int page,
            int size
    );
}
