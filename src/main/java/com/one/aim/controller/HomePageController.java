package com.one.aim.controller;

import com.one.aim.rs.HomePageRs;
import com.one.aim.service.HomePageService;
import com.one.vm.core.BaseDataRs;
import com.one.vm.core.BaseRs;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/home")
@RequiredArgsConstructor
public class HomePageController {

    private final HomePageService homePageService;

    @GetMapping
    public ResponseEntity<BaseRs> getHome(
            @RequestParam(defaultValue = "6") int limit
    ) {
        HomePageRs data = homePageService.getHomePage(limit);

        BaseRs rs = new BaseRs();
        rs.setStatus("SUCCESS");
        rs.setData(new BaseDataRs("Home loaded", data));

        return ResponseEntity.ok(rs);

    }
}
