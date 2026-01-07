package com.one.aim.rq;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomReportRq {

    // Metrics
    private boolean revenue;
    private boolean orders;
    private boolean users;

    // Filters
    private String category;
    private Long sellerId;

    // Time range
    private LocalDate startDate;
    private LocalDate endDate;

    // Pagination
    private int page = 0;
    private int size = 10;
}

