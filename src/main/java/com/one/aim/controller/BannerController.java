package com.one.aim.controller;

import com.one.aim.constants.ContentStatus;
import com.one.aim.rq.BannerRq;
import com.one.aim.rs.BannerRs;
import com.one.aim.rs.PagedRs;
import com.one.aim.service.BannerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin/banners")
@RequiredArgsConstructor
public class BannerController {

    private final BannerService bannerService;

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<BannerRs> createBanner(
            @RequestPart("banner") @Valid BannerRq rq,
            @RequestPart(value = "image", required = false) MultipartFile image) throws Exception {

        BannerRs response = bannerService.createBanner(rq, image);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    public ResponseEntity<BannerRs> updateBanner(
            @PathVariable Long id,
            @RequestPart("banner") @Valid BannerRq rq,
            @RequestPart(value = "image", required = false) MultipartFile image) throws Exception {

        BannerRs response = bannerService.updateBanner(id, rq, image);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BannerRs> getBannerById(@PathVariable Long id) {
        BannerRs response = bannerService.getBannerById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<PagedRs<BannerRs>> getAllBanners(
            @RequestParam(required = false) ContentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "priority") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("ASC") ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        PagedRs<BannerRs> response = status != null ?
                bannerService.getBannersByStatus(status, pageable) :
                bannerService.getAllBanners(pageable);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/active")
    public ResponseEntity<List<BannerRs>> getActiveBanners(
            @RequestParam(required = false) String position) {
        List<BannerRs> response = bannerService.getActiveBanners(position);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBanner(@PathVariable Long id) {
        bannerService.deleteBanner(id);
        return ResponseEntity.noContent().build();
    }
}
