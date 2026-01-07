package com.one.vm.analytics;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AnalyticsSummaryVm {

    // ----- VALUES -----
    private Double totalSales;
    private Double averageOrderValue;
    private Long customerAcquisition;
    private Integer customerRetentionPercent;

    // ----- GROWTH % -----
    private Double totalSalesGrowthPercent;
    private Double averageOrderValueGrowthPercent;
    private Double customerAcquisitionGrowthPercent;
    private Double customerRetentionGrowthPercent;
}


