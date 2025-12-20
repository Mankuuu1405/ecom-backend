package com.one.aim.service;

import org.springframework.http.ResponseEntity;

public interface ChargesService {

    /**
     * Calculate all charges for user's cart
     * Includes category-wise tax, shipping, discount, payment charge
     */
    ResponseEntity<?> calculate(
            Long userId,
            Long addressId,
            String promoCode
    ) throws Exception;
}


