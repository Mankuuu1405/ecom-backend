package com.one.aim.repo;

import com.one.aim.bo.BlogBO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlogRepo extends JpaRepository<BlogBO,Long> {

    Page<BlogBO> findByActiveTrueOrderByCreatedAtDesc(Pageable pageable);

}
