package com.one.aim.rs.data;

import com.one.aim.rs.CategoryCardRs;
import com.one.aim.rs.ProductCardRs;
import com.one.aim.rs.RecommendationRs;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HomePageDataRs {

    private List<CategoryCardRs> categories;
    private List<RecommendationRs> trending;
    private List<ProductCardRs> newArrivals;

    private List<RecommendationRs> topElectronics;
    private List<RecommendationRs> topFashion;
    private List<RecommendationRs> topHomeKitchen;
}

