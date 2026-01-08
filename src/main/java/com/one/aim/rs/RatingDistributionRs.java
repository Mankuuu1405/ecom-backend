package com.one.aim.rs;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RatingDistributionRs {
    private Integer stars;
    private Integer count;
    private Integer percentage;
}
