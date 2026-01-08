package com.one.aim.repo;

import com.one.aim.bo.BlogBO;
import com.one.aim.constants.ContentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BlogRepo extends JpaRepository<BlogBO,Long> {

//    Page<BlogBO> findByActiveTrueOrderByCreatedAtDesc(Pageable pageable);

    Optional<BlogBO> findBySlug(String slug);

    Page<BlogBO> findByStatus(ContentStatus status, Pageable pageable);

    @Query("SELECT b FROM BlogBO b WHERE b.status = :status AND b.scheduledPublishAt <= :now")
    List<BlogBO> findScheduledBlogs(@Param("status") ContentStatus status, @Param("now") LocalDateTime now);

    @Query("SELECT b FROM BlogBO b WHERE " +
            "LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(b.content) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(b.keywords) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<BlogBO> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    Page<BlogBO> findAll(Specification<BlogBO> spec, Pageable pageable);
}
