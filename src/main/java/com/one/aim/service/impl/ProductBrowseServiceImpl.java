package com.one.aim.service.impl;

import com.one.aim.bo.ProductBO;
import com.one.aim.mapper.ProductMapper;
import com.one.aim.repo.ProductRepo;
import com.one.aim.rs.ProductCardRs;
import com.one.aim.service.CategoryService;
import com.one.aim.service.ProductBrowseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductBrowseServiceImpl implements ProductBrowseService {

    private final ProductRepo productRepository;
    private final ProductMapper productMapper;
    private final CategoryService categoryService;

    // =====================================================
    // UNIFIED SEARCH (USED BY SEARCH + SHOP PAGE)
    // =====================================================
    @Override
    public Page<ProductCardRs> search(
            String q,
            List<Long> categoryIds,
            String gender,
            List<String> brands,
            Integer minPrice,
            Integer maxPrice,
            Integer rating,
            String sort,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(parseSort(sort)));

        Specification<ProductBO> spec = Specification
                .where(activeOnly())
                .and(nameLike(q))
                .and(categoryIn(categoryIds))
                .and(genderEquals(gender))
                .and(brandIn(brands))
                .and(priceBetween(minPrice, maxPrice))
                .and(ratingAtLeast(rating));

        return productRepository
                .findAll(spec, pageable)
                .map(productMapper::toCardRs);
    }

    // =====================================================
    // BROWSE (LEGACY SUPPORT – SINGLE CATEGORY / BRAND)
    // =====================================================
    @Override
    public Page<ProductCardRs> browse(
            String category,   // slug
            String brand,
            Integer minPrice,
            Integer maxPrice,
            Integer rating,
            String sort,
            int page,
            int size
    ) {
        List<Long> categoryIds = null;
        List<String> brands = null;

        // slug → categoryId
        if (category != null && !category.isBlank()) {
            Long id = categoryService.getIdBySlug(category);
            categoryIds = List.of(id);
        }

        if (brand != null && !brand.isBlank()) {
            brands = List.of(brand);
        }

        // Delegate to unified search
        return search(
                null,           // q
                categoryIds,    // ALWAYS list
                null,           // gender
                brands,         // ALWAYS list
                minPrice,
                maxPrice,
                rating,
                sort,
                page,
                size
        );
    }

    // =====================================================
    // SPECIFICATIONS
    // =====================================================

    private Specification<ProductBO> activeOnly() {
        return (root, query, cb) -> cb.isTrue(root.get("active"));
    }

    /**
     * Search by:
     * - product name
     * - brand
     */
    private Specification<ProductBO> nameLike(String q) {
        if (q == null || q.isBlank()) return null;

        String like = "%" + q.toLowerCase() + "%";

        return (root, query, cb) ->
                cb.or(
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("brand")), like)
                );
    }

    private Specification<ProductBO> categoryIn(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) return null;

        return (root, query, cb) ->
                root.get("categoryId").in(categoryIds);
    }

    private Specification<ProductBO> genderEquals(String gender) {
        if (gender == null || gender.isBlank()) return null;

        return (root, query, cb) ->
                cb.equal(root.get("gender"), gender);
    }

    // MULTI-BRAND SUPPORT
    private Specification<ProductBO> brandIn(List<String> brands) {
        if (brands == null || brands.isEmpty()) return null;

        return (root, query, cb) ->
                root.get("brand").in(brands);
    }

    private Specification<ProductBO> priceBetween(Integer minPrice, Integer maxPrice) {
        if (minPrice == null && maxPrice == null) return null;

        return (root, query, cb) -> {
            if (minPrice != null && maxPrice != null) {
                return cb.between(root.get("price"), minPrice, maxPrice);
            }
            if (minPrice != null) {
                return cb.greaterThanOrEqualTo(root.get("price"), minPrice);
            }
            return cb.lessThanOrEqualTo(root.get("price"), maxPrice);
        };
    }

    private Specification<ProductBO> ratingAtLeast(Integer rating) {
        if (rating == null) return null;

        return (root, query, cb) ->
                cb.greaterThanOrEqualTo(
                        root.get("averageRating"),
                        rating.doubleValue()
                );
    }

    private Sort.Order parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return new Sort.Order(Sort.Direction.DESC, "createdAt");
        }

        String[] parts = sort.split(",");
        String field = parts[0];
        Sort.Direction direction =
                parts.length > 1 && parts[1].equalsIgnoreCase("asc")
                        ? Sort.Direction.ASC
                        : Sort.Direction.DESC;

        return new Sort.Order(direction, field);
    }


}

