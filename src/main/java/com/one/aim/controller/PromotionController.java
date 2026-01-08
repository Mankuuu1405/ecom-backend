package com.one.aim.controller;

import com.one.aim.constants.ContentStatus;
import com.one.aim.rq.PromotionRq;
import com.one.aim.rs.PagedRs;
import com.one.aim.rs.PromotionRs;
import com.one.aim.service.PromotionService;
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
@RequestMapping("/api/admin/promotions")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<PromotionRs> createPromotion(
            @RequestPart("promotion") @Valid PromotionRq rq,
            @RequestPart(value = "image", required = false) MultipartFile image) throws Exception {

        PromotionRs response = promotionService.createPromotion(rq, image);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    public ResponseEntity<PromotionRs> updatePromotion(
            @PathVariable Long id,
            @RequestPart("promotion") @Valid PromotionRq rq,
            @RequestPart(value = "image", required = false) MultipartFile image) throws Exception {

        PromotionRs response = promotionService.updatePromotion(id, rq, image);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PromotionRs> getPromotionById(@PathVariable Long id) {
        PromotionRs response = promotionService.getPromotionById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<PagedRs<PromotionRs>> getAllPromotions(
            @RequestParam(required = false) ContentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "priority") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("ASC") ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        PagedRs<PromotionRs> response = status != null ?
                promotionService.getPromotionsByStatus(status, pageable) :
                promotionService.getAllPromotions(pageable);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/active")
    public ResponseEntity<List<PromotionRs>> getActivePromotions() {
        List<PromotionRs> response = promotionService.getActivePromotions();
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePromotion(@PathVariable Long id) {
        promotionService.deletePromotion(id);
        return ResponseEntity.noContent().build();
    }
}
