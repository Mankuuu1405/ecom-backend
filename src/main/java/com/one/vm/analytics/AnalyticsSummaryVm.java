package com.one.vm.analytics;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AnalyticsSummaryVm {
    private Double totalSales;
    private Double averageOrderValue;
    private Long customerAcquisition;
    private Integer customerRetentionPercent;
}

