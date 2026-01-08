package com.one.aim.rs;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HomePageRs {

    private List<BannerRs> banners;
    private List<HomeSectionRs> sections;
}

