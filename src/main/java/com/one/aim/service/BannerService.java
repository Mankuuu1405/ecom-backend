package com.one.aim.service;

import com.one.aim.constants.ContentStatus;
import com.one.aim.rq.BannerRq;
import com.one.aim.rs.BannerRs;
import com.one.aim.rs.PagedRs;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface BannerService {
    BannerRs createBanner(BannerRq rq, MultipartFile image) throws Exception;

    BannerRs updateBanner(Long id, BannerRq rq, MultipartFile image) throws Exception;

    BannerRs getBannerById(Long id);

    PagedRs<BannerRs> getAllBanners(Pageable pageable);

    PagedRs<BannerRs> getBannersByStatus(ContentStatus status, Pageable pageable);

    List<BannerRs> getActiveBanners(String position);

    void deleteBanner(Long id);

    void processScheduledBanners();
}
