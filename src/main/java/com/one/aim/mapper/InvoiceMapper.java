package com.one.aim.mapper;

import com.one.aim.bo.InvoiceBO;
import com.one.aim.bo.OrderItemBO;
import com.one.aim.bo.ProductBO;
import com.one.aim.bo.SellerBO;
import com.one.aim.rs.AdminInvoiceRs;
import com.one.aim.rs.InvoiceRs;

import java.util.List;
import java.util.Objects;

public class InvoiceMapper {

    // Convert entity to USER invoice DTO
    public static InvoiceRs toDto(InvoiceBO inv) {
        return new InvoiceRs(
                inv.getId(),
                inv.getInvoiceNumber(),
                inv.getOrder().getOrderId(),
                inv.getOrder().getOrderTime().toString(),
                inv.getOrder().getTotalAmount()
        );
    }

    // Convert entity to ADMIN invoice DTO
    public static AdminInvoiceRs toAdminDto(InvoiceBO inv) {

        List<Long> sellerIds = inv.getOrder().getOrderItems()
                .stream()
                .map(OrderItemBO::getProduct)
                .filter(Objects::nonNull)
                .map(ProductBO::getSeller)
                .filter(Objects::nonNull)
                .map(SellerBO::getId)
                .distinct()
                .toList();

        return new AdminInvoiceRs(
                inv.getId(),
                inv.getInvoiceNumber(),
                inv.getOrder().getOrderId(),
                inv.getOrder().getOrderTime().toString(),
                inv.getOrder().getTotalAmount(),
                inv.getUser().getId(),
                sellerIds
        );
    }
}

