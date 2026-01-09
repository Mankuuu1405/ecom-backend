package com.one.aim.rs;

import lombok.*;

@Getter @Setter
@AllArgsConstructor
@Builder
public class ReviewCardRs {
    private String userName;
    private int rating;
    private String comment;
}