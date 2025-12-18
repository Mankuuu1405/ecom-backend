package com.one.aim.controller;

import com.one.aim.rs.ProductCardRs;
import com.one.aim.rs.ProductDetailsRs;
import com.one.aim.service.ProductBrowseService;
import com.one.aim.service.ProductService;
import com.one.vm.core.BaseDataRs;
import com.one.vm.core.BaseRs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/public/product")
@Slf4j
@RequiredArgsConstructor
public class PublicProductController {

    private final ProductService productService;
    private final ProductBrowseService productBrowseService;


    @GetMapping("/share/{slug}")
    public ResponseEntity<?> getShareMessage(@PathVariable String slug) throws Exception {

        String msg = productService.getShareableProduct(slug);

        BaseRs rs = new BaseRs();
        rs.setStatus("SUCCESS");
        rs.setData(new BaseDataRs("Share message", msg));

        return ResponseEntity.ok(rs);
    }


    // ===========================================================
    // PUBLIC: PRODUCT DETAILS (SEPARATE FROM SHARE TO AVOID CONFLICT)
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
    // PUBLIC: GET PRODUCT IMAGES BY PRODUCT ID
    // ===========================================================
    @GetMapping("/{productId}/images")
    public ResponseEntity<?> getProductImages(@PathVariable Long productId) throws Exception {

        log.debug("REST [GET /api/public/product/{}/images]", productId);

        return ResponseEntity.ok(productService.getProductImages(productId));
    }


    // ===========================================================
    // PUBLIC: LIST ALL PRODUCTS (Pagination + Sorting)
    // ===========================================================
//    @GetMapping("/browse")
//    public ResponseEntity<?> getProducts(
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "20") int size,
//            @RequestParam(defaultValue = "createdAt,desc") String sort
//    ) throws Exception {
//
//        Page<ProductCardRs> data = productService.getProducts(null, page, size, sort);
//        return ResponseEntity.ok(data);
//    }


    // ===========================================================
    // PUBLIC: LIST PRODUCTS BY CATEGORY (Category Slug Support)
    // ===========================================================
    @GetMapping("/category/{categorySlug}")
    public ResponseEntity<?> getProductsByCategory(
            @PathVariable String categorySlug,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "20") int limit
    ) throws Exception {

        log.debug("REST [GET /api/public/product/category/{}]", categorySlug);

        return ResponseEntity.ok(
                productService.getProductsByCategory(categorySlug, offset, limit)
        );
    }


    // ===========================================================
    // PUBLIC: SEARCH PRODUCTS BY NAME
    // ===========================================================
    @GetMapping("/search")
    public ResponseEntity<?> searchProducts(
            @RequestParam String name,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "20") int limit
    ) throws Exception {

        log.debug("REST [GET /api/public/product/search]");

        return ResponseEntity.ok(
                productService.searchProducts(name, offset, limit)
        );
    }


    // ===========================================================
    // PUBLIC: FILTER PRODUCTS (Category, Price, Brand, Rating, Sort)
    // ===========================================================
    @GetMapping
    public ResponseEntity<Page<ProductCardRs>> browseProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(required = false) Integer rating,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {

        Page<ProductCardRs> data =
                productBrowseService.browse(
                        category, brand,
                        minPrice, maxPrice,
                        rating, sort,
                        page, size
                );

        return ResponseEntity.ok(data);
    }

    // Best Sellers
    @GetMapping("/best-seller")
    public ResponseEntity<?> bestSellers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        return ResponseEntity.ok(productService.getBestSellers(page, size, sort));
    }

    // New Arrivals
    @GetMapping("/new-arrival")
    public ResponseEntity<?> newArrivals(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        return ResponseEntity.ok(productService.getNewArrivals(page, size, sort));
    }

    // Sale Products
    @GetMapping("/sale")
    public ResponseEntity<?> saleProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "price,asc") String sort
    ) {
        return ResponseEntity.ok(productService.getSaleProducts(page, size, sort));
    }


}
