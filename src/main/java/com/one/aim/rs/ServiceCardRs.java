package com.one.aim.rs;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServiceCardRs {

    private Long id;
    private String title;
    private String description;
    private String image;     // file URL: /api/files/public/{id}/view
    private Double startingPrice;
    private String slug;
}

