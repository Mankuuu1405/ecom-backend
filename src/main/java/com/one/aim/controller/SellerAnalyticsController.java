package com.one.aim.controller;

import com.one.aim.rs.SellerAnalyticsRs;
import com.one.aim.service.SellerAnalyticsService;
import com.one.vm.analytics.SalesTrendVm;
import com.one.vm.analytics.TopProductChartVm;
import com.one.vm.core.BaseRs;
import com.one.vm.utils.ResponseUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.PrintWriter;
import java.time.LocalDateTime;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;


@RestController
@RequestMapping("/api/seller/analytics")
@RequiredArgsConstructor
@Slf4j
public class SellerAnalyticsController {

    private final SellerAnalyticsService sellerAnalyticsService;


    @GetMapping
    @PreAuthorize("hasAuthority('SELLER')")
    public BaseRs getAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) String category
    ) {
        return ResponseUtils.success(
                sellerAnalyticsService.getAnalytics(from, to, category)

        );
    }

    @GetMapping("/export/csv")
    @PreAuthorize("hasAuthority('SELLER')")
    public void exportCsv(
            HttpServletResponse response,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to
    ) throws Exception {

        response.setContentType("text/csv");
        response.setHeader(
                "Content-Disposition",
                "attachment; filename=seller-analytics.csv"
        );

        SellerAnalyticsRs data =
                sellerAnalyticsService.getAnalytics(from, to, null);

        PrintWriter writer = response.getWriter();

        writer.println("Date,Sales");

        for (SalesTrendVm s : data.getSalesTrend()) {
            writer.println(s.getDay() + "," + s.getSales());
        }

        writer.flush();
    }



    @GetMapping("/export/pdf")
    @PreAuthorize("hasAuthority('SELLER')")
    public void exportPdf(
            HttpServletResponse response,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to
    ) throws Exception {

        response.setContentType("application/pdf");
        response.setHeader(
                "Content-Disposition",
                "attachment; filename=seller-analytics.pdf"
        );

        SellerAnalyticsRs data =
                sellerAnalyticsService.getAnalytics(from, to, null);

        Document document = new Document();
        PdfWriter.getInstance(document, response.getOutputStream());

        document.open();

        document.add(new Paragraph("Seller Analytics Report"));
        document.add(new Paragraph(" "));

        for (TopProductChartVm p : data.getProductPerformance()) {
            document.add(
                    new Paragraph(p.getName() + " : " + p.getUnits())
            );
        }

        document.close();
    }




//    @GetMapping("/charts/sales")
//    @PreAuthorize("hasAuthority('SELLER')")
//    public BaseRs getSalesTrend() {
//        SellerAnalyticsRs analytics = sellerAnalyticsService.getAnalytics();
//        List<SalesTrendVm> salesTrend = analytics == null ? List.of() : analytics.getSalesTrend();
//        return ResponseUtils.success(salesTrend);
//    }
//
//
//    @GetMapping("/reports/products")
//    @PreAuthorize("hasAuthority('SELLER')")
//    public BaseRs getProductPerformance() {
//
//        SellerAnalyticsRs analytics =
//                sellerAnalyticsService.getAnalytics();
//
//        List<TopProductChartVm> topProducts =
//                analytics == null
//                        ? List.of()
//                        : analytics.getProductPerformance();
//
//        return ResponseUtils.success(topProducts);
//    }

}

