package com.one.aim.controller;

import com.one.aim.rs.ReviewRs;
import com.one.aim.service.ProductService;
import com.one.vm.core.BaseDataRs;
import com.one.vm.core.BaseRs;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

//@RestController
//@RequestMapping("/api/public/products")
//@RequiredArgsConstructor
//public class ProductReviewController {
//
//    private final ProductService productService;
//
//    @GetMapping("/{id}/reviews")
//    public ResponseEntity<BaseRs> getProductReviews(
//            @PathVariable Long id,
//            Pageable pageable) {
//
//        Page<ReviewRs> reviews =
//                productService.getProductReviews(id, pageable);
//
//        BaseRs rs = new BaseRs();
//        rs.setStatus("SUCCESS");
//        rs.setData(new BaseDataRs("Product reviews fetched", reviews));
//
//        return ResponseEntity.ok(rs);
//    }
//}
//
