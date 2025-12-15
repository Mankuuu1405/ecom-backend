package com.one.aim.controller;

import com.one.aim.rs.ServiceCardRs;
import com.one.aim.service.ServiceModuleService;
import com.one.vm.core.BaseDataRs;
import com.one.vm.core.BaseRs;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/service")
@RequiredArgsConstructor
public class PublicServiceController {

    private final ServiceModuleService serviceModuleService;

    // ================================================
    // GET TOP SERVICES
    // ================================================
    @GetMapping
    public ResponseEntity<BaseRs> getTopServices(
            @RequestParam(defaultValue = "10") int limit
    ) {

        List<ServiceCardRs> list = serviceModuleService.getTopServices(limit);

        BaseRs rs = new BaseRs();
        rs.setStatus("SUCCESS");
        rs.setData(new BaseDataRs("Top services", list));

        return ResponseEntity.ok(rs);
    }

    // ================================================
    // GET SERVICE DETAILS BY SLUG
    // ================================================
    @GetMapping("/{slug}")
    public ResponseEntity<BaseRs> getServiceDetail(@PathVariable String slug) {

        ServiceCardRs data = serviceModuleService.getServiceDetails(slug);

        BaseRs rs = new BaseRs();
        rs.setStatus("SUCCESS");
        rs.setData(new BaseDataRs("Service details", data));

        return ResponseEntity.ok(rs);
    }
}



