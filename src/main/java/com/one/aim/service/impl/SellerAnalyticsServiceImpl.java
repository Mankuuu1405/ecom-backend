package com.one.aim.service.impl;

import com.one.aim.repo.OrderItemBORepo;
import com.one.aim.rs.*;
import com.one.aim.service.SellerAnalyticsService;
import com.one.utils.AuthUtils;
import com.one.vm.analytics.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SellerAnalyticsServiceImpl implements SellerAnalyticsService {

    private final OrderItemBORepo orderItemRepo;

    @Override
    @Cacheable(
            value = "sellerAnalytics",
            key = "#root.target.getSellerId() + ':' + #from + ':' + #to + ':' + #category"
    )
    public SellerAnalyticsRs getAnalytics(
            LocalDateTime from,
            LocalDateTime to,
            String category
    ) {

        Long sellerId = getSellerId();

        // --------------------
        // DATE RANGES
        // --------------------
        LocalDateTime end = (to != null) ? to : LocalDateTime.now();
        LocalDateTime start = (from != null) ? from : end.minusDays(30);

        LocalDateTime prevEnd = start;
        LocalDateTime prevStart = start.minusDays(30);

        // --------------------
        // CURRENT PERIOD (NULL SAFE)
        // --------------------
        double totalSales = safeDouble(
                orderItemRepo.getTotalSalesBySeller(
                        sellerId, start, end, category
                )
        );

        long totalOrders = safeLong(
                orderItemRepo.getTotalOrdersBySellerWithCategory(
                        sellerId, start, end, category
                )
        );

        double avgOrderValue =
                totalOrders == 0 ? 0 : totalSales / totalOrders;

        long customerAcquisition = safeLong(
                orderItemRepo.getUniqueCustomersBySellerWithCategory(
                        sellerId, start, end, category
                )
        );

        int retentionPercent = safeInt(
                orderItemRepo.getCustomerRetentionPercent(
                        sellerId, start, end
                )
        );

        // --------------------
        // PREVIOUS PERIOD (NULL SAFE)
        // --------------------
        double prevTotalSales = safeDouble(
                orderItemRepo.getTotalSalesBySeller(
                        sellerId, prevStart, prevEnd, category
                )
        );

        long prevTotalOrders = safeLong(
                orderItemRepo.getTotalOrdersBySellerWithCategory(
                        sellerId, prevStart, prevEnd, category
                )
        );

        double prevAvgOrderValue =
                prevTotalOrders == 0 ? 0 : prevTotalSales / prevTotalOrders;

        long prevCustomerAcquisition = safeLong(
                orderItemRepo.getUniqueCustomersBySellerWithCategory(
                        sellerId, prevStart, prevEnd, category
                )
        );

        int prevRetentionPercent = safeInt(
                orderItemRepo.getCustomerRetentionPercent(
                        sellerId, prevStart, prevEnd
                )
        );

        // --------------------
        // GROWTH CALCULATIONS (SAFE)
        // --------------------
        double totalSalesGrowthPercent =
                calculateGrowth(totalSales, prevTotalSales);

        double avgOrderValueGrowthPercent =
                calculateGrowth(avgOrderValue, prevAvgOrderValue);

        double customerAcquisitionGrowthPercent =
                calculateGrowth(customerAcquisition, prevCustomerAcquisition);

        double retentionGrowthPercent =
                calculateGrowth(retentionPercent, prevRetentionPercent);

        // --------------------
        // SUMMARY
        // --------------------
        AnalyticsSummaryVm summary =
                new AnalyticsSummaryVm(
                        totalSales,
                        BigDecimal.valueOf(avgOrderValue)
                                .setScale(2, RoundingMode.HALF_UP)
                                .doubleValue(),
                        customerAcquisition,
                        retentionPercent,

                        totalSalesGrowthPercent,
                        avgOrderValueGrowthPercent,
                        customerAcquisitionGrowthPercent,
                        retentionGrowthPercent
                );

        // --------------------
        // SALES TREND
        // --------------------
        List<SalesTrendVm> salesTrend =
                orderItemRepo.getSellerDailySales(sellerId, start, end, category)
                        .stream()
                        .map(r -> new SalesTrendVm(
                                r[0].toString(),
                                ((Number) r[1]).doubleValue()
                        ))
                        .toList();

        // --------------------
        // PRODUCT PERFORMANCE
        // --------------------
        List<TopProductChartVm> products =
                orderItemRepo.findTopSellingBySeller(
                                sellerId,
                                start,
                                end,
                                category,
                                PageRequest.of(0, 5)
                        )
                        .stream()
                        .map(r -> new TopProductChartVm(
                                r[1].toString(),
                                ((Number) r[2]).longValue()
                        ))
                        .toList();

        // --------------------
        // ORDER STATUS
        // --------------------
        List<OrderStatusVm> status =
                orderItemRepo.getSellerOrderStatusSummary(
                                sellerId, start, end, category
                        )
                        .stream()
                        .map(r -> new OrderStatusVm(
                                r[0].toString(),
                                ((Number) r[1]).intValue()
                        ))
                        .toList();

        // --------------------
        // CUSTOMER ACTIVITY
        // --------------------
        List<DailyOrderCountVm> activity =
                orderItemRepo.getSellerDailyOrderCount(
                                sellerId, start, end, category
                        )
                        .stream()
                        .map(r -> new DailyOrderCountVm(
                                r[0].toString(),
                                ((Number) r[1]).intValue()
                        ))
                        .toList();

        return new SellerAnalyticsRs(
                summary,
                salesTrend,
                products,
                status,
                activity
        );
    }

    // --------------------
    // UTIL METHODS
    // --------------------
    private double calculateGrowth(double current, double previous) {
        if (previous <= 0) return 0.0;
        return ((current - previous) / previous) * 100;
    }

    private double safeDouble(Double value) {
        return value == null ? 0.0 : value;
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    public Long getSellerId() {
        return AuthUtils.findLoggedInUser().getDocId();
    }
}
