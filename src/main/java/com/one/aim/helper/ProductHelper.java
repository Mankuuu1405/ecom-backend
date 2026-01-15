package com.one.aim.helper;

import com.one.aim.rq.ProductRq;
import com.one.utils.Utils;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ProductHelper {

    public static List<String> validateProduct(ProductRq rq) {
        List<String> errors = new ArrayList<>();

        try {
            if (Utils.isEmpty(rq.getName())) {
                errors.add("EC_REQUIRED_PRODUCT_NAME");
            }

            if (Utils.isEmpty(rq.getDescription())) {
                errors.add("EC_REQUIRED_DESCRIPTION");
            }

            if (rq.getPrice() == null || rq.getPrice() <= 0) {
                errors.add("EC_INVALID_PRICE");
            }

            if (rq.getStock() == null || rq.getStock() < 1) {
                errors.add("EC_INVALID_STOCK");
            }

            /* ================= SALE VALIDATION ================= */
            if (Boolean.TRUE.equals(rq.getOnSale())) {

                Double price = rq.getPrice();
                Double offerPrice = rq.getOfferPrice();
                Integer discountPercent = rq.getDiscountPercent();

                // both provided
                if (offerPrice != null && discountPercent != null) {
                    errors.add("EC_BOTH_OFFER_AND_DISCOUNT_PROVIDED");
                }

                // none provided
                if (offerPrice == null && discountPercent == null) {
                    errors.add("EC_OFFER_OR_DISOUNT_REQUIRED");
                }

                // offer price validation
                if (offerPrice != null) {
                    if (offerPrice <= 0 || offerPrice >= price) {
                        errors.add("EC_INVALID_OFFER_PRICE");
                    }
                }

                // discount percent validation
                if (discountPercent != null) {
                    if (discountPercent <= 0 || discountPercent >= 100) {
                        errors.add("EC_INVALID_DISCOUNT_PERCENT");
                    }
                }
            }

        } catch (Exception e) {
            log.error("Exception in validateProduct(ProductRq) -> ", e);
            errors.add("EC_INTERNAL_ERROR");
        }

        return errors;
    }
}
