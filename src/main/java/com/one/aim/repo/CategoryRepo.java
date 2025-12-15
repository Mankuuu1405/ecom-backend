package com.one.aim.repo;

import com.one.aim.bo.CategoryBO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepo extends JpaRepository<CategoryBO, Long> {

//    boolean existsByNameIgnoreCase(String name);
//
//    // For pagination (active categories)
//    Page<CategoryBO> findByActiveTrue(Pageable pageable);

    // For homepage "featured/browse categories"
    List<CategoryBO> findByActiveTrueOrderByCreatedAtAsc();

    // When accessing category page: we need detail from slug
//    Optional<CategoryBO> findBySlug(String slug);

    // Homepage: top categories with pagination
    @Query("SELECT c FROM CategoryBO c WHERE c.active = true ORDER BY c.createdAt ASC")
    List<CategoryBO> findTopActiveCategories(Pageable pageable);

    List<CategoryBO> findByActiveTrue();

    Page<CategoryBO> findByActiveTrue(Pageable pageable);

    Optional<CategoryBO> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsByNameIgnoreCase(String name);

    Optional<CategoryBO> findByNameIgnoreCase(String name);


}

