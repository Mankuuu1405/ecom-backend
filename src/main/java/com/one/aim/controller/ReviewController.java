package com.one.aim.controller;

import com.one.aim.rq.ReviewRq;
import com.one.aim.rq.ReviewUpdateRq;
import com.one.aim.rs.ReviewRs;
import com.one.aim.service.ProductService;
import com.one.aim.service.impl.ReviewService;
import com.one.vm.core.BaseDataRs;
import com.one.vm.core.BaseRs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Slf4j
public class ReviewController {

    private final ReviewService reviewService;
    private final ProductService productService;

    // ================================
    // ADD REVIEW (USER ONLY)
    // ================================
    @PreAuthorize("hasAuthority('USER')")
    @PostMapping("/add")
    public ResponseEntity<BaseRs> addReview(@RequestBody ReviewRq rq) {

        reviewService.addReview(rq);

        BaseRs rs = new BaseRs();
        rs.setStatus("SUCCESS");
        rs.setData(new BaseDataRs("Review added successfully", null));

        return ResponseEntity.ok(rs);
    }


    // ================================
    // GET REVIEWS BY PRODUCT (PUBLIC)
    // ================================
    @GetMapping("/public/product/{slug}")
    public BaseRs getProductReviews(
            @PathVariable String slug,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        Page<ReviewRs> reviews = reviewService.getReviewsByProductSlug(slug, page, size);

        BaseRs rs = new BaseRs();
        rs.setStatus("SUCCESS");
        rs.setData(new BaseDataRs("Reviews retrieved successfully", reviews));
        return rs;
    }

    // ================================
    // UPDATE REVIEW (USER ONLY)
    // ================================
    @PreAuthorize("hasAuthority('USER')")
    @PutMapping("/update")
    public BaseRs updateReview(@RequestBody ReviewUpdateRq rq) {

        reviewService.updateReview(rq);

        BaseRs rs = new BaseRs();
        rs.setStatus("SUCCESS");
        rs.setData(new BaseDataRs("Review updated successfully", null));
        return rs;
    }

    // ================================
    // DELETE REVIEW (USER/ADMIN)
    // ================================
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    @DeleteMapping("/{id}")
    public BaseRs deleteReview(@PathVariable Long id) {

        reviewService.deleteReview(id);

        BaseRs rs = new BaseRs();
        rs.setStatus("SUCCESS");
        rs.setData(new BaseDataRs("Review deleted successfully", null));
        return rs;
    }

    // ================================
    // GET USER'S REVIEWS (USER ONLY)
    // ================================
    @PreAuthorize("hasAuthority('USER')")
    @GetMapping("/my-reviews")
    public BaseRs getMyReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        Page<ReviewRs> reviews = reviewService.getUserReviews(page, size);

        BaseRs rs = new BaseRs();
        rs.setStatus("SUCCESS");
        rs.setData(new BaseDataRs("Your reviews retrieved successfully", reviews));
        return rs;
    }

    // ================================
    // LIKE REVIEW
    // ================================
    @PostMapping("/{id}/like")
    public BaseRs likeReview(@PathVariable Long id) {

        reviewService.likeReview(id);

        BaseRs rs = new BaseRs();
        rs.setStatus("SUCCESS");
        rs.setData(new BaseDataRs("Review liked", null));
        return rs;
    }

    // ================================
    // DISLIKE REVIEW
    // ================================
    @PostMapping("/{id}/dislike")
    public BaseRs dislikeReview(@PathVariable Long id) {

        reviewService.dislikeReview(id);

        BaseRs rs = new BaseRs();
        rs.setStatus("SUCCESS");
        rs.setData(new BaseDataRs("Review disliked", null));
        return rs;
    }
}