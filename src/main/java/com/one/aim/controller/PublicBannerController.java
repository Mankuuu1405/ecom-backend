package com.one.aim.controller;

import com.one.aim.rs.BannerRs;
import com.one.aim.service.BannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/banners")
@RequiredArgsConstructor
public class PublicBannerController {

    private final BannerService bannerService;

    @GetMapping("/active")
    public ResponseEntity<List<BannerRs>> getActiveBanners(
            @RequestParam(required = false) String position) {
        List<BannerRs> response = bannerService.getActiveBanners(position);
        return ResponseEntity.ok(response);
    }
}

