package com.one.aim.rs;

import com.one.vm.analytics.*;
import lombok.*;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SellerAnalyticsRs {

    private AnalyticsSummaryVm summary;
    private List<SalesTrendVm> salesTrend;
    private List<TopProductChartVm> productPerformance;
    private List<OrderStatusVm> orderStatus;
    private List<DailyOrderCountVm> customerActivity;                // Line chart
}

