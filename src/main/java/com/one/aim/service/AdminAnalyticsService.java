package com.one.aim.service;

import com.one.aim.rq.CustomReportRq;
import com.one.aim.rs.*;
import com.one.vm.core.BaseRs;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.List;

public interface AdminAnalyticsService {

    // Existing (unchanged)
    AdminAnalyticsRs getDashboard();

    // New enhanced APIs
    SummaryCardsRs getOverview(LocalDate startDate, LocalDate endDate);

    SalesPerformanceRs getSalesChart(LocalDate startDate, LocalDate endDate);

    UserActivityRs getUserActivityChart(LocalDate startDate, LocalDate endDate);

    Page<SalesTableRowRs> getSalesPerformanceReport(
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size
    );

    Page<UserActivityRowRs> getUserActivityReport(
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size
    );

    Page<CustomReportRowRs> generateCustomReport(CustomReportRq rq);

    Page<?> getCustomReport(
            String type,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size
    );

    byte[] exportCustomReport(CustomReportRq rq);

}

