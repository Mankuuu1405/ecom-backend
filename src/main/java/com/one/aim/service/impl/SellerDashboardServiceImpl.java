package com.one.aim.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;


import com.one.aim.bo.SellerBO;
import com.one.aim.repo.ProductRepo;
import com.one.aim.repo.SellerRepo;
import com.one.aim.rs.SellerOverviewRs;
import com.one.vm.analytics.TopProductVm;
import com.one.utils.AuthUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.one.aim.repo.OrderRepo;
import com.one.aim.service.SellerDashboardService;
import com.one.vm.common.RecentOrderVm;
import com.one.vm.core.BaseDataRs;
import com.one.vm.core.BaseRs;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SellerDashboardServiceImpl implements SellerDashboardService {

    private final OrderRepo orderRepo;
    private final SellerRepo sellerRepo;
    private final ProductRepo productRepo;

    @Override
    public BaseRs getSellerOverview() {

        SellerBO seller = sellerRepo.findByEmail(
                AuthUtils.findLoggedInUser().getEmail()
        ).orElseThrow(() -> new RuntimeException("Seller not found"));

        Long sellerId = seller.getId();

        // ---------- TOTAL ----------
        double totalRevenue =
                safeDouble(orderRepo.getTotalRevenueBySeller(sellerId));

        long totalOrders =
                safeLong(orderRepo.getSellerOrderCount(sellerId));


        int totalProducts =
                Math.toIntExact(
                        safeLong(productRepo.countProductsBySeller(sellerId))
                );

        double avgOrderValueRaw =
                totalOrders == 0 ? 0 : totalRevenue / totalOrders;

        double averageOrderValue =
                BigDecimal.valueOf(avgOrderValueRaw)
                        .setScale(2, RoundingMode.HALF_UP)
                        .doubleValue();

        // ---------- DATE RANGES ----------
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime last30Start = now.minusDays(30);
        LocalDateTime prev30Start = now.minusDays(60);

        // ---------- REVENUE ----------
        double revenueLast30 =
                safeDouble(orderRepo.getRevenueBetween(sellerId, last30Start, now));

        double revenuePrev30 =
                safeDouble(orderRepo.getRevenueBetween(sellerId, prev30Start, last30Start));

        // ---------- ORDERS ----------
        long ordersLast30 =
                safeLong(orderRepo.getOrdersBetween(sellerId, last30Start, now));

        long ordersPrev30 =
                safeLong(orderRepo.getOrdersBetween(sellerId, prev30Start, last30Start));

        double avgOrderValuePrev =
                ordersPrev30 == 0 ? 0 : revenuePrev30 / ordersPrev30;

        double avgOrderValueGrowth =
                calculateGrowth(averageOrderValue, avgOrderValuePrev);


        // ---------- GROWTH ----------
        double revenueGrowth =
                calculateGrowth(revenueLast30, revenuePrev30);

        double orderGrowth =
                calculateGrowth(ordersLast30, ordersPrev30);

        // ---------- RECENT ORDERS ----------
        List<RecentOrderVm> recentOrders =
                orderRepo.findRecentOrders(sellerId, PageRequest.of(0, 5))
                        .getContent()
                        .stream()
                        .map(r -> new RecentOrderVm(
                                r[0].toString(),
                                r[1].toString(),
                                ((LocalDateTime) r[2]).withNano(0),
                                mapStatus(r[3].toString()),
                                ((Number) r[4]).doubleValue()
                        ))
                        .toList();

        // ---------- TOP PRODUCTS ----------
        List<TopProductVm> topProducts =
                orderRepo.getTopProducts(sellerId)
                        .stream()
                        .map(r -> new TopProductVm(
                                r[0].toString(),
                                ((Number) r[1]).intValue()
                        ))
                        .toList();

        SellerOverviewRs overview =
                new SellerOverviewRs(
                        "INR",
                        new SellerOverviewRs.Stats(
                                totalRevenue,                 // double
                                totalOrders,                  // long
                                averageOrderValue,            // double

                                revenueGrowth,                // Double
                                orderGrowth,                  // Double

                                avgOrderValueGrowth,           // double

                                totalProducts                 // int
                        ),
                        recentOrders,
                        topProducts
                );


        return success("Seller dashboard fetched successfully", overview);
    }

    // ---------- UTIL METHODS ----------

    private double calculateGrowth(double current, double previous) {
        if (previous == 0) {
            return 0.0; // NEVER return null
        }
        return ((current - previous) / previous) * 100;
    }

    private String mapStatus(String status) {
        return switch (status) {
            case "INITIAL" -> "Processing";
            case "PLACED" -> "Shipped";
            case "DELIVERED" -> "Delivered";
            default -> status;
        };
    }

    private BaseRs success(String msg, Object data) {
        BaseRs rs = new BaseRs();
        rs.setStatus("SUCCESS");
        rs.setData(new BaseDataRs(msg, data));
        return rs;
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private double safeDouble(Double value) {
        return value == null ? 0.0 : value;
    }

    private double safeDouble(Long value) {
        return value == null ? 0.0 : value.doubleValue();
    }

}

