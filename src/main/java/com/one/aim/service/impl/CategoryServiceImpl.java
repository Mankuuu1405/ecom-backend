package com.one.aim.service.impl;

import com.one.aim.bo.CategoryBO;
import com.one.aim.bo.FileBO;
import com.one.aim.constants.ErrorCodes;
import com.one.aim.mapper.CategoryMapper;
import com.one.aim.repo.CategoryRepo;
import com.one.aim.repo.ProductRepo;
import com.one.aim.rq.CategoryRq;
import com.one.aim.rs.CategoryCardRs;
import com.one.aim.rs.CategoryRs;
import com.one.aim.service.CategoryService;
import com.one.aim.service.FileService;
import com.one.exception.BaseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepo categoryRepo;
    private final ProductRepo productRepo;
    private final FileService fileService;
    private final CategoryMapper categoryMapper;

    @Override
    public CategoryRs createCategory(CategoryRq rq) {
        if (categoryRepo.existsByNameIgnoreCase(rq.getName())) {
            throw new BaseException(ErrorCodes.EC_CREATE_FAILED, "Category already exists");
        }

        CategoryBO bo = categoryMapper.toEntity(rq);
        CategoryBO saved = categoryRepo.save(bo);

        return categoryMapper.toRs(saved);
    }

    @Override
    public CategoryRs updateCategory(CategoryRq rq) {
        CategoryBO bo = categoryRepo.findById(rq.getId())
                .orElseThrow(() -> new BaseException(ErrorCodes.EC_RECORD_NOT_FOUND, "Category not found"));

        // Check if name is being changed to an existing name
        if (!bo.getName().equalsIgnoreCase(rq.getName()) &&
                categoryRepo.existsByNameIgnoreCase(rq.getName())) {
            throw new BaseException(ErrorCodes.EC_CREATE_FAILED, "Category name already exists");
        }

        categoryMapper.updateEntity(bo, rq);
        CategoryBO saved = categoryRepo.save(bo);

        return categoryMapper.toRs(saved);
    }

    @Override
    public List<CategoryRs> getAllCategories() {
        return categoryRepo.findAll()
                .stream()
                .map(categoryMapper::toRs)
                .toList();
    }

    @Override
    public List<CategoryRs> getActiveCategories() {
        return categoryRepo.findByActiveTrue()
                .stream()
                .map(categoryMapper::toRs)
                .toList();
    }

    @Override
    public void deleteCategory(Long id) {
        if (!categoryRepo.existsById(id)) {
            throw new BaseException(ErrorCodes.EC_RECORD_NOT_FOUND, "Category not found");
        }

        // Check if category has products
        Long productCount = productRepo.countByCategoryId(id);
        if (productCount > 0) {
            throw new BaseException(ErrorCodes.EC_DELETE_FAILED,
                    "Cannot delete category with existing products. Deactivate it instead.");
        }

        categoryRepo.deleteById(id);
    }

    @Override
    public void deactivateCategory(Long id) {
        CategoryBO bo = categoryRepo.findById(id)
                .orElseThrow(() -> new BaseException(ErrorCodes.EC_RECORD_NOT_FOUND, "Category not found"));

        bo.setActive(false);
        categoryRepo.save(bo);
    }

    @Override
    public Page<CategoryCardRs> getCategories(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        Page<CategoryBO> categories = categoryRepo.findByActiveTrue(pageable);

        // Bulk count for efficiency (1 query instead of N)
        List<Object[]> rows = productRepo.countProductsGroupedByCategory();
        Map<Long, Long> countMap = rows.stream()
                .collect(Collectors.toMap(
                        r -> (Long) r[0],
                        r -> (Long) r[1]
                ));

        return categories.map(cat -> {
            Long count = countMap.getOrDefault(cat.getId(), 0L);
            return categoryMapper.toCardRs(cat, count);
        });
    }

    @Override
    @Transactional
    public CategoryRs uploadCategoryImage(Long id, MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Image file is required");
        }

        // Validate size <= 2MB
        if (file.getSize() > 2 * 1024 * 1024) {
            throw new RuntimeException("Max image size is 2MB");
        }

        // Validate type JPG/PNG
        String type = file.getContentType();
        if (!List.of("image/jpeg", "image/png").contains(type)) {
            throw new RuntimeException("Only JPG or PNG allowed");
        }

        CategoryBO bo = categoryRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        // Delete previous image if exists
        if (bo.getImageFileId() != null) {
            try {
                fileService.deleteFileById(String.valueOf(bo.getImageFileId()));
            } catch (Exception e) {
                log.warn("Failed to delete old category image: {}", e.getMessage());
            }
        }

        FileBO uploaded = fileService.uploadAndReturnFile(file);
        bo.setImageFileId(uploaded.getId());
        categoryRepo.save(bo);

        return categoryMapper.toRs(bo);
    }

    @Override
    @Transactional
    public CategoryRs deleteCategoryImage(Long id) throws Exception {
        CategoryBO bo = categoryRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        if (bo.getImageFileId() != null) {
            try {
                fileService.deleteFileById(String.valueOf(bo.getImageFileId()));
            } catch (Exception e) {
                log.warn("Failed to delete category image: {}", e.getMessage());
            }
            bo.setImageFileId(null);
            categoryRepo.save(bo);
        }

        return categoryMapper.toRs(bo);
    }

    @Override
    public CategoryCardRs getCategoryDetails(String slug) {
        CategoryBO bo = categoryRepo.findBySlug(slug)
                .orElseThrow(() -> new BaseException(ErrorCodes.EC_RECORD_NOT_FOUND, "Category not found"));

        if (!bo.isActive()) {
            throw new BaseException(ErrorCodes.EC_RECORD_NOT_FOUND, "Category is not active");
        }

        Long count = productRepo.countActiveProductsByCategory(bo.getId());
        return categoryMapper.toBrowseRs(bo, count);

    }

    @Override
    public List<CategoryCardRs> getAllForBrowse() {
        List<CategoryBO> categories = categoryRepo.findByActiveTrue();

        return categories.stream()
                .map(cat -> {
                    Long count = productRepo.countByActiveTrueAndCategoryNameIgnoreCase(cat.getName());
                    return categoryMapper.toBrowseRs(cat, count);
                })
                .filter(cat -> cat.getProductCount() > 0)
                .sorted(Comparator.comparing(CategoryCardRs::getName))
                .toList();

    }
}