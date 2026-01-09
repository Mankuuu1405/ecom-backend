package com.one.aim.service.impl;

import com.one.aim.bo.BannerBO;
import com.one.aim.bo.FileBO;
import com.one.aim.constants.ContentStatus;
import com.one.aim.mapper.BannerMapper;
import com.one.aim.repo.BannerRepo;
import com.one.aim.rq.BannerRq;
import com.one.aim.rs.BannerRs;
import com.one.aim.rs.PagedRs;
import com.one.aim.service.BannerService;
import com.one.aim.service.FileService;
import com.one.exception.ResourceNotFoundException;
import com.one.utils.UrlUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BannerServiceImpl implements BannerService {

    private final BannerRepo bannerRepository;
    private final BannerMapper bannerMapper;
    private final FileService fileService;
    private final UrlUtils urlUtils;

    @Override
    @Transactional
    public BannerRs createBanner(BannerRq rq, MultipartFile image) throws Exception {
        // Handle image upload if provided
        if (image != null && !image.isEmpty()) {
            FileBO uploadedFile = fileService.uploadAndReturnFile(image);
            rq.setImageFileId(uploadedFile.getId());
        }

        BannerBO banner = bannerMapper.toEntity(rq);
        BannerBO savedBanner = bannerRepository.save(banner);
        log.info("Banner created with ID: {}", savedBanner.getId());
        return toResponseWithImageUrl(savedBanner);
    }

    @Override
    @Transactional
    public BannerRs updateBanner(Long id, BannerRq rq, MultipartFile image) throws Exception {
        BannerBO banner = bannerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Banner not found with ID: " + id));

        if (image != null && !image.isEmpty()) {
            if (banner.getImageFileId() != null) {
                try { fileService.deleteFile(banner.getImageFileId()); }
                catch (Exception e) { log.warn("Failed to delete old image: {}", e.getMessage()); }
            }
            FileBO uploadedFile = fileService.uploadAndReturnFile(image);
            rq.setImageFileId(uploadedFile.getId());
        } else {
            rq.setImageFileId(banner.getImageFileId()); // keep old
        }

        bannerMapper.updateEntityFromDTO(rq, banner);
        BannerBO updated = bannerRepository.save(banner);
        return bannerMapper.toResponseDTO(updated);
    }


    @Override
    @Transactional(readOnly = true)
    public BannerRs getBannerById(Long id) {
        BannerBO banner = bannerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Banner not found with ID: " + id));
        return toResponseWithImageUrl(banner);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedRs<BannerRs> getAllBanners(Pageable pageable) {
        Page<BannerBO> bannerPage = bannerRepository.findAll(pageable);
        return buildPagedResponse(bannerPage);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedRs<BannerRs> getBannersByStatus(ContentStatus status, Pageable pageable) {
        Page<BannerBO> bannerPage = bannerRepository.findByStatus(status, pageable);
        return buildPagedResponse(bannerPage);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BannerRs> getActiveBanners(String position) {
        List<BannerBO> banners;
        if (position != null && !position.trim().isEmpty()) {
            banners = bannerRepository.findByStatusAndPositionOrderByPriorityDesc(
                    ContentStatus.PUBLISHED, position
            );
        } else {
            banners = bannerRepository.findActiveBanners(ContentStatus.PUBLISHED, LocalDateTime.now());
        }

        return banners.stream()
                .map(this::toResponseWithImageUrl)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteBanner(Long id) {
        BannerBO banner = bannerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Banner not found with ID: " + id));

        // Delete associated image if exists
        if (banner.getImageFileId() != null) {
            try {
                fileService.deleteFile(banner.getImageFileId());
            } catch (Exception e) {
                log.warn("Failed to delete banner image: {}", e.getMessage());
            }
        }

        bannerRepository.deleteById(id);
        log.info("Banner deleted with ID: {}", id);
    }

    @Override
    @Transactional
    public void processScheduledBanners() {
        LocalDateTime now = LocalDateTime.now();
        List<BannerBO> allBanners = bannerRepository.findAll();

        allBanners.forEach(banner -> {
            if (banner.getStatus() == ContentStatus.SCHEDULED &&
                    banner.getScheduledPublishAt() != null &&
                    banner.getScheduledPublishAt().isBefore(now)) {
                banner.setStatus(ContentStatus.PUBLISHED);
            }

            if (banner.getStatus() == ContentStatus.PUBLISHED &&
                    banner.getScheduledUnpublishAt() != null &&
                    banner.getScheduledUnpublishAt().isBefore(now)) {
                banner.setStatus(ContentStatus.ARCHIVED);
            }
        });

        bannerRepository.saveAll(allBanners);
        log.info("Processed scheduled banners");
    }

    private BannerRs toResponseWithImageUrl(BannerBO banner) {
        BannerRs rs = bannerMapper.toResponseDTO(banner);
        if (banner.getImageFileId() != null) {
            rs.setImageUrl(urlUtils.publicFile(banner.getImageFileId()));
        } else {
            rs.setImageUrl(urlUtils.defaultBanner());
        }
        return rs;
    }

    private PagedRs<BannerRs> buildPagedResponse(Page<BannerBO> page) {
        List<BannerRs> content = page.getContent().stream()
                .map(this::toResponseWithImageUrl)
                .collect(Collectors.toList());

        return PagedRs.<BannerRs>builder()
                .content(content)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .first(page.isFirst())
                .build();
    }
}