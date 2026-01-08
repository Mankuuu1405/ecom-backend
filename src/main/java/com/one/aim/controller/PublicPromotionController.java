package com.one.aim.controller;

import com.one.aim.rs.PromotionRs;
import com.one.aim.service.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/promotions")
@RequiredArgsConstructor
public class PublicPromotionController {

    private final PromotionService promotionService;

    @GetMapping("/active")
    public ResponseEntity<List<PromotionRs>> getActivePromotions() {
        List<PromotionRs> response = promotionService.getActivePromotions();
        return ResponseEntity.ok(response);
    }
}
