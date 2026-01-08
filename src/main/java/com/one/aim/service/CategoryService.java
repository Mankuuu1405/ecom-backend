package com.one.aim.service;

import com.one.aim.rq.CategoryRq;
import com.one.aim.rs.CategoryCardRs;
import com.one.aim.rs.CategoryRs;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CategoryService {

    CategoryRs createCategory(CategoryRq rq, MultipartFile image) throws Exception;

    CategoryRs updateCategory(CategoryRq rq);

    List<CategoryRs> getAllCategories();           // admin view

    List<CategoryRs> getActiveCategories();        // seller dropdown

    void deleteCategory(Long id);                  // hard delete

    void deactivateCategory(Long id);              // soft delete

    Page<CategoryCardRs> getCategories(int page, int size);

    CategoryRs uploadCategoryImage(Long id, MultipartFile file) throws Exception;

    CategoryRs deleteCategoryImage(Long id) throws Exception;

    CategoryCardRs getCategoryDetails(String slug);

    List<CategoryCardRs> getAllForBrowse();

    List<CategoryCardRs> getPopularCategories();

    CategoryRs updateCategoryImage(Long id, MultipartFile file) throws Exception;

    Long getIdBySlug(String slug);

}
