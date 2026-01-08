package com.one.aim.rs;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Builder
public class PdpRs {

    private ProductDetailsRs product;
    private Page<ReviewRs> reviews;
    private RecommendationBlockRs recommendations;
}

