package com.one.aim.repo;

import com.one.aim.bo.BannerBO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BannerRepo extends JpaRepository<BannerBO,Integer> {
    @Query("""
           SELECT b
           FROM BannerBO b
           WHERE b.active = true
           ORDER BY b.priority ASC, b.createdAt DESC
           """)
    List<BannerBO> findActiveBanners();
}
