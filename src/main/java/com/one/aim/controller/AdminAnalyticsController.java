package com.one.aim.controller;

import com.one.aim.rq.CustomReportRq;
import com.one.aim.service.AdminAnalyticsService;
import com.one.vm.core.BaseRs;
import com.one.vm.utils.ResponseUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/api/admin/analytics")
@RequiredArgsConstructor
public class AdminAnalyticsController {

    private final AdminAnalyticsService adminAnalyticsService;

    // ================= DASHBOARD (UNCHANGED) =================
    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('ADMIN')")
    public BaseRs getDashboard() {
        return ResponseUtils.success(adminAnalyticsService.getDashboard());
    }

    // ================= OVERVIEW (DATE RANGE) =================
    @GetMapping("/overview")
    @PreAuthorize("hasAuthority('ADMIN')")
    public BaseRs getOverview(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate
    ) {
        return ResponseUtils.success(
                adminAnalyticsService.getOverview(startDate, endDate)
        );
    }

    // ================= SALES CHART =================
    @GetMapping("/charts/sales")
    @PreAuthorize("hasAuthority('ADMIN')")
    public BaseRs getSalesChart(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate
    ) {
        return ResponseUtils.success(
                adminAnalyticsService.getSalesChart(startDate, endDate)
        );
    }

    // ================= USER ACTIVITY CHART =================
    @GetMapping("/charts/user-activity")
    @PreAuthorize("hasAuthority('ADMIN')")
    public BaseRs getUserActivityChart(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate
    ) {
        return ResponseUtils.success(
                adminAnalyticsService.getUserActivityChart(startDate, endDate)
        );
    }

    // ================= SALES TABLE (PAGINATED) =================
    @GetMapping("/reports/sales-performance")
    @PreAuthorize("hasAuthority('ADMIN')")
    public BaseRs getSalesPerformanceReport(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseUtils.success(
                adminAnalyticsService.getSalesPerformanceReport(
                        startDate, endDate, page, size
                )
        );
    }

    // ================= USER ACTIVITY TABLE (PAGINATED) =================
    @GetMapping("/reports/user-activity")
    @PreAuthorize("hasAuthority('ADMIN')")
    public BaseRs getUserActivityReport(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseUtils.success(
                adminAnalyticsService.getUserActivityReport(
                        startDate, endDate, page, size
                )
        );
    }

    // ------------------------------------------------------------
// CUSTOM REPORT GENERATOR
// ------------------------------------------------------------
    @PostMapping("/reports/custom/export")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<byte[]> exportCustomReport(
            @RequestBody CustomReportRq rq
    ) {
        byte[] file = adminAnalyticsService.exportCustomReport(rq);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=custom-report.csv")
                .contentType(MediaType.TEXT_PLAIN)
                .body(file);
    }


    @GetMapping("/reports/custom")
    @PreAuthorize("hasAuthority('ADMIN')")
    public BaseRs getCustomReport(
            @RequestParam String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseUtils.success(
                adminAnalyticsService.getCustomReport(
                        type, startDate, endDate, page, size
                )
        );
    }



}
