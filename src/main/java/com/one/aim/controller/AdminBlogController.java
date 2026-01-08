package com.one.aim.controller;

import com.one.aim.rq.BlogCreateRq;
import com.one.aim.service.BlogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

//@RestController
//@RequestMapping("/api/admin/blogs")
//@RequiredArgsConstructor
//@Transactional
//public class AdminBlogController {
//
//    private final BlogService blogService;
//
//    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    public ResponseEntity<?> createBlog(
//            @ModelAttribute BlogCreateRq rq,
//            @RequestPart("image") MultipartFile image
//    ) throws Exception {
//        blogService.createBlog(rq, image);
//        return ResponseEntity.ok("Blog created successfully");
//    }
//}

