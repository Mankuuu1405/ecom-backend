package com.one.aim.rs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.one.vm.analytics.TopProductVm;
import com.one.vm.common.RecentOrderVm;
import lombok.*;

import java.util.List;

@Getter
@AllArgsConstructor
public class SellerOverviewRs {

    private String currency;
    private Stats stats;
    private List<RecentOrderVm> recentOrders;
    private List<TopProductVm> topProducts;

    @Getter
    @AllArgsConstructor
    public static class Stats {
        private double totalRevenue;
        private long totalOrders;
        private double averageOrderValue;

        private Double revenueGrowthPercent;
        private Double orderGrowthPercent;


        private int totalProducts;
    }
}




