package com.one.aim.repo;

import com.one.aim.bo.ServiceBO;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceRepo extends JpaRepository<ServiceBO, Long> {

    Optional<ServiceBO> findBySlug(String slug);

    @Query("SELECT s FROM ServiceBO s WHERE s.active = true ORDER BY s.createdAt DESC")
    List<ServiceBO> findActiveServices(Pageable pageable);
}

