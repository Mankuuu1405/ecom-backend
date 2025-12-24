package com.one.aim.rs;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomReportRowRs {

    private String label;     // product / category / seller
    private Long revenue;
    private Long orders;
    private Long users;
}

