package com.one.aim.controller;

import com.one.aim.rs.ProductCardRs;
import com.one.aim.rs.ProductDetailsRs;
import com.one.aim.rs.ProductFacetRs;
import com.one.aim.rs.ReviewRs;
import com.one.aim.service.*;
import com.one.vm.core.BaseDataRs;
import com.one.vm.core.BaseRs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/public/product")
@Slf4j
@RequiredArgsConstructor
public class PublicProductController {

    private final ProductService productService;
    private final ProductBrowseService productBrowseService;
    private final ProductAutocompleteService productAutocompleteService;
    private final ProductFacetService productFacetService;
    private final CategoryService categoryService;

    // ===========================================================
    // SHARE PRODUCT
    // ===========================================================
    @GetMapping("/share/{slug}")
    public ResponseEntity<?> getShareMessage(@PathVariable String slug) throws Exception {

        String msg = productService.getShareableProduct(slug);

        BaseRs rs = new BaseRs();
        rs.setStatus("SUCCESS");
        rs.setData(new BaseDataRs("Share message", msg));

        return ResponseEntity.ok(rs);
    }

    // ===========================================================
    // PRODUCT DETAILS
    // ===========================================================
    @GetMapping("/details/{slug}")
    public ResponseEntity<?> getProductDetails(@PathVariable String slug) throws Exception {

        ProductDetailsRs data = productService.getProductDetails(slug);

        BaseRs rs = new BaseRs();
        rs.setStatus("SUCCESS");
        rs.setData(new BaseDataRs("Product details retrieved successfully", data));

        return ResponseEntity.ok(rs);
    }

    // ===========================================================
    // PRODUCT IMAGES
    // ===========================================================
    @GetMapping("/{productId}/images")
    public ResponseEntity<?> getProductImages(@PathVariable Long productId) throws Exception {

        log.debug("REST [GET /api/public/product/{}/images]", productId);
        return ResponseEntity.ok(productService.getProductImages(productId));
    }

    // ===========================================================
    // UNIFIED SEARCH (MAIN API)
    // ===========================================================
    @GetMapping("/search")
    public ResponseEntity<Page<ProductCardRs>> searchProducts(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category, // comma-separated slugs
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String brand,    // comma-separated brands
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(required = false) Integer rating,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        List<Long> categoryIds = null;
        List<String> brands = null;

        // Convert category slugs to IDs
        if (category != null && !category.isBlank()) {
            categoryIds = Arrays.stream(category.split(","))
                    .map(String::trim)
                    .map(categoryService::getIdBySlug)
                    .toList();
        }

        // Parse multiple brands
        if (brand != null && !brand.isBlank()) {
            brands = Arrays.stream(brand.split(","))
                    .map(String::trim)
                    .toList();
        }

        return ResponseEntity.ok(
                productBrowseService.search(
                        q,
                        categoryIds,
                        gender,
                        brands,
                        minPrice,
                        maxPrice,
                        rating,
                        sort,
                        page,
                        size
                )
        );
    }


    // ===========================================================
    // BROWSE PRODUCTS (BACKWARD COMPATIBLE)
    // ===========================================================
    @GetMapping
    public ResponseEntity<Page<ProductCardRs>> browseProducts(
            @RequestParam(required = false) String category,   // sofas,beds
            @RequestParam(required = false) String brand,      // Ikea,Urban
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(required = false) Integer rating,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {

        List<Long> categoryIds = null;
        List<String> brands = null;

        // MULTI CATEGORY (slug → id)
        if (category != null && !category.isBlank()) {
            categoryIds = Arrays.stream(category.split(","))
                    .map(String::trim)
                    .map(categoryService::getIdBySlug)
                    .toList();
        }

        // MULTI BRAND
        if (brand != null && !brand.isBlank()) {
            brands = Arrays.stream(brand.split(","))
                    .map(String::trim)
                    .toList();
        }

        return ResponseEntity.ok(
                productBrowseService.search(
                        null,           // q
                        categoryIds,    // MULTI
                        null,           // gender
                        brands,         // MULTI
                        minPrice,
                        maxPrice,
                        rating,
                        sort,
                        page,
                        size
                )
        );
    }




    // ===========================================================
// HOME SECTIONS
// ===========================================================

    @GetMapping("/best-seller")
    public ResponseEntity<?> bestSellers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "soldCount,desc") String sort
    ) {
        return ResponseEntity.ok(
                productService.getBestSellers(page, size, sort)
        );
    }

    @GetMapping("/new-arrival")
    public ResponseEntity<?> newArrivals(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        return ResponseEntity.ok(
                productService.getNewArrivals(page, size, sort)
        );
    }

    @GetMapping("/sale")
    public ResponseEntity<?> saleProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "discountPercent,desc") String sort
    ) {
        return ResponseEntity.ok(
                productService.getSaleProducts(page, size, sort)
        );
    }

    // ===========================================================
// FACET COUNTS (BRANDS + PRICE RANGE)
// ===========================================================
    @GetMapping("/facets")
    public ResponseEntity<ProductFacetRs> getFacets(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(required = false) Integer rating
    ) {

        List<Long> categoryIds = null;
        if (category != null && !category.isBlank()) {
            categoryIds = Arrays.stream(category.split(","))
                    .map(String::trim)
                    .map(categoryService::getIdBySlug)
                    .toList();
        }

        List<String> brands = null;
        if (brand != null && !brand.isBlank()) {
            brands = Arrays.stream(brand.split(","))
                    .map(String::trim)
                    .toList();
        }

        return ResponseEntity.ok(
                productFacetService.getFacets(
                        q,
                        categoryIds,
                        brands,
                        minPrice,
                        maxPrice,
                        rating
                )
        );
    }




    // ===========================================================
// AUTOCOMPLETE SEARCH
// ===========================================================
    @GetMapping("/autocomplete")
    public ResponseEntity<?> autocomplete(
            @RequestParam String q
    ) {
        return ResponseEntity.ok(
                productAutocompleteService.autocomplete(q)
        );
    }

}
