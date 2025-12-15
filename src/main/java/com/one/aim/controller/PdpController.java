package com.one.aim.controller;

import com.one.aim.rs.PdpRs;
import com.one.aim.service.PdpService;
import com.one.vm.core.BaseDataRs;
import com.one.vm.core.BaseRs;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/pdp")
@RequiredArgsConstructor
public class PdpController {

    private final PdpService pdpService;

    @GetMapping("/{slug}")
    public ResponseEntity<BaseRs> getPdp(@PathVariable String slug) throws Exception {

        PdpRs data = pdpService.getPdpBySlug(slug);

        BaseRs rs = new BaseRs();
        rs.setStatus("SUCCESS");
        rs.setData(new BaseDataRs("PDP loaded", data));

        return ResponseEntity.ok(rs);

    }
}

