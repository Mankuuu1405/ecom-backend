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

        LocalDateTime end = to != null ? to : LocalDateTime.now();
        LocalDateTime start = from != null ? from : end.minusDays(30);

        // --------------------
        // SUMMARY CARDS
        // --------------------
        Double totalSales =
                orderItemRepo.getTotalSalesBySeller(
                        sellerId, start, end, category
                );

        Long totalOrders =
                orderItemRepo.getTotalOrdersBySellerWithCategory(
                        sellerId, start, end, category
                );

        double avgOrderValue =
                (totalOrders == null || totalOrders == 0)
                        ? 0
                        : totalSales / totalOrders;

        Long customerAcquisition =
                orderItemRepo.getUniqueCustomersBySellerWithCategory(
                        sellerId, start, end, category
                );

        Integer retentionPercent =
                orderItemRepo.getCustomerRetentionPercent(
                        sellerId, start, end
                );

        AnalyticsSummaryVm summary =
                new AnalyticsSummaryVm(
                        totalSales == null ? 0 : totalSales,
                        BigDecimal.valueOf(avgOrderValue)
                                .setScale(2, RoundingMode.HALF_UP)
                                .doubleValue(),
                        customerAcquisition == null ? 0 : customerAcquisition,
                        retentionPercent // ✅ keep null if no previous data
                );

        // --------------------
        // SALES TREND (NO CATEGORY)
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
        // PRODUCT PERFORMANCE (NO CATEGORY HERE)
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
        // ORDER STATUS PIE (NO CATEGORY)
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
        // CUSTOMER ACTIVITY (NO CATEGORY)
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
    // CACHE KEY HELPER
    // --------------------
    public Long getSellerId() {
        return AuthUtils.findLoggedInUser().getDocId();
    }
}
