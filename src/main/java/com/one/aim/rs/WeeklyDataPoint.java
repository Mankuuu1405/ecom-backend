package com.one.aim.rs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyDataPoint {
    private String label;  // "Week 1", "Week 2", etc.
    private Long value;    // revenue or count
}
