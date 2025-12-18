package com.one.aim.controller;

import com.one.aim.rq.ServiceCreateRq;
import com.one.aim.service.ServiceModuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/services")
@RequiredArgsConstructor
@Transactional
public class AdminServiceController {

    private final ServiceModuleService serviceModuleService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createService(
            @ModelAttribute ServiceCreateRq rq,
            @RequestPart("image") MultipartFile image
    ) throws Exception {
        serviceModuleService.createService(rq, image);
        return ResponseEntity.ok("Service created successfully");
    }
}

