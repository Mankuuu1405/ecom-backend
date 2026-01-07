package com.one.aim.rs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RatingSummaryRs {
    private Double average;
    private Long totalReviews;
    private List<RatingDistributionRs> distribution;
}

