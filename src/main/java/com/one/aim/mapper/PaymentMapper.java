package com.one.aim.mapper;

import com.one.aim.bo.PaymentBO;
import com.one.aim.rs.PaymentRs;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PaymentMapper {

    private PaymentMapper() {}

    public static PaymentRs mapToPaymentRs(PaymentBO bo) {

        if (bo == null) {
            log.warn("PaymentBO is NULL");
            return null;
        }

        PaymentRs rs = new PaymentRs();
        rs.setId(bo.getId());
        rs.setAmount(bo.getAmount());               // rupees
        rs.setPaymentMethod(bo.getPaymentMethod());
        rs.setStatus(bo.getStatus());
        rs.setRazorpayOrderId(bo.getRazorpayOrderId());
        rs.setRazorpayPaymentId(bo.getRazorpayPaymentId());
        rs.setPaymentTime(bo.getPaymentTime());

        return rs;
    }
}

