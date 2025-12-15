package com.one.aim.service.impl;

import com.one.aim.rs.ProductCardRs;
import com.one.aim.service.ProductBrowseService;
import com.one.aim.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductBrowseServiceImpl implements ProductBrowseService {

    private final ProductService productService;

    @Override
    public Page<ProductCardRs> browse(
            String category,
            String brand,
            Integer minPrice,
            Integer maxPrice,
            Integer rating,
            String sort,
            int page,
            int size
    ) {
        return productService.filterProducts(
                category,
                brand,
                minPrice,
                maxPrice,
                rating,
                sort,
                page,
                size
        );
    }
}

