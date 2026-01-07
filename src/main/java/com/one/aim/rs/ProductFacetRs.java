package com.one.aim.rs;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
public class ProductFacetRs {
    private List<BrandCountRs> brands;
    private PriceRangeRs price;
}

