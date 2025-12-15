package com.one.aim.rs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RecommendationBlockRs {

    private List<ProductCardRs> peopleAlsoBought;
    private List<ProductCardRs> frequentlyBoughtTogether;
    private List<ProductCardRs> similarProducts;

}
