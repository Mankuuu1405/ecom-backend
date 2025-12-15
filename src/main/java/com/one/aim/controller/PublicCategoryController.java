package com.one.aim.controller;

import com.one.aim.rs.CategoryCardRs;
import com.one.aim.rs.ProductCardRs;
import com.one.aim.service.CategoryService;
import com.one.aim.service.ProductService;
import com.one.vm.core.BaseDataRs;
import com.one.vm.core.BaseRs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/category")
@RequiredArgsConstructor
@Slf4j
public class PublicCategoryController {

    private final CategoryService categoryService;
    private final ProductService productService;

    // ---------------------------------------------------------
    // 1. LIST ACTIVE CATEGORIES (PAGINATED)
    // ---------------------------------------------------------
    @GetMapping
    public ResponseEntity<?> getCategories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) throws Exception {

        log.debug("REST [GET /api/public/category]");

        Page<CategoryCardRs> data = categoryService.getCategories(page, size);
        return ResponseEntity.ok(data);
    }

    // ---------------------------------------------------------
    // 2. BROWSE ALL CATEGORIES (NO PAGINATION, for Figma "Browse")
    // ---------------------------------------------------------
    @GetMapping("/browse")
    public ResponseEntity<?> browseCategories() throws Exception {

        log.debug("REST [GET /api/public/category/browse]");

        List<CategoryCardRs> data = categoryService.getAllForBrowse();
        return ResponseEntity.ok(data);
    }

    // ---------------------------------------------------------
    // 3. CATEGORY DETAILS BY SLUG (Name, image, productCount)
    // ---------------------------------------------------------
    @GetMapping("/{slug}")
    public ResponseEntity<?> getCategoryDetails(@PathVariable String slug) throws Exception {

        CategoryCardRs data = categoryService.getCategoryDetails(slug);

        BaseRs rs = new BaseRs();
        rs.setStatus("SUCCESS");
        rs.setData(new BaseDataRs("Category details", data));

        return ResponseEntity.ok(rs);
    }


    // ---------------------------------------------------------
    // 4. PRODUCTS UNDER CATEGORY (by slug)
    // ---------------------------------------------------------
    @GetMapping("/{slug}/products")
    public ResponseEntity<?> getProductsByCategory(
            @PathVariable String slug,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) throws Exception {

        log.debug("REST [GET /api/public/category/{}/products]", slug);

        Page<ProductCardRs> data = productService.getProducts(slug, page, size, sort);
        return ResponseEntity.ok(data);
    }
}


