package com.one.aim.controller;

import com.one.aim.rs.RecommendationRs;
import com.one.aim.service.RecommendationService;
import com.one.vm.core.BaseDataRs;
import com.one.vm.core.BaseRs;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/recommend")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recService;

    @GetMapping("/frequently-bought-together/{productId}")
    public ResponseEntity<BaseRs> fbt(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "5") int limit
    ) {

        List<RecommendationRs> list =
                recService.getFrequentlyBoughtTogetherSimple(productId, limit);

        BaseRs rs = new BaseRs();
        rs.setStatus("SUCCESS");
        rs.setData(new BaseDataRs("Frequently bought together", list));

        return ResponseEntity.ok(rs);
    }

    @GetMapping("/people-also-bought/{productId}")
    public ResponseEntity<BaseRs> pab(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "10") int limit
    ) {

        List<RecommendationRs> list =
                recService.getPeopleAlsoBoughtSimple(productId, limit);

        BaseRs rs = new BaseRs();
        rs.setStatus("SUCCESS");
        rs.setData(new BaseDataRs("People also bought", list));

        return ResponseEntity.ok(rs);
    }

    @GetMapping("/recommended")
    @PreAuthorize("hasAuthority('USER')")
    public ResponseEntity<BaseRs> recommended(
            @RequestParam(defaultValue = "10") int limit
    ) {

        List<RecommendationRs> list =
                recService.getRecommendedForUserSimple(limit);

        BaseRs rs = new BaseRs();
        rs.setStatus("SUCCESS");
        rs.setData(new BaseDataRs("Recommended for you", list));

        return ResponseEntity.ok(rs);
    }

    @GetMapping("/trending")
    public ResponseEntity<BaseRs> trending(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "10") int limit
    ) {

        List<RecommendationRs> list =
                recService.getTrendingSimple(days, limit);

        BaseRs rs = new BaseRs();
        rs.setStatus("SUCCESS");
        rs.setData(new BaseDataRs("Trending products", list));

        return ResponseEntity.ok(rs);
    }

    @GetMapping("/category/{categorySlug}")
    public ResponseEntity<BaseRs> topByCategory(
            @PathVariable String categorySlug,
            @RequestParam(defaultValue = "10") int limit
    ) {

        List<RecommendationRs> list =
                recService.getTopByCategorySimple(categorySlug, limit);

        BaseRs rs = new BaseRs();
        rs.setStatus("SUCCESS");
        rs.setData(new BaseDataRs("Top products in category", list));

        return ResponseEntity.ok(rs);
    }
}

