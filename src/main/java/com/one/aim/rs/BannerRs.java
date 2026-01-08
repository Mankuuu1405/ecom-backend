package com.one.aim.rs;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BannerRs {
    private Long id;
    private String title;
    private String subtitle;
    private String image;
    private String buttonText;
    private String buttonLink;
}

