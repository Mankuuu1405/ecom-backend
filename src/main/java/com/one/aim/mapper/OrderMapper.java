package com.one.aim.mapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.one.aim.bo.*;
import com.one.aim.rs.OrderItemRs;
import com.one.aim.rs.OrderRs;
import com.one.aim.rs.OrderSummaryRs;
import com.one.aim.service.FileService;
import com.one.utils.UrlUtils;
import com.one.utils.Utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class OrderMapper {

    private final UserMapper userMapper;
    private final UrlUtils urlUtils;

    public OrderRs mapToOrderRs(OrderBO bo) {

        if (bo == null) return null;

        OrderRs rs = new OrderRs();
        rs.setDocId(String.valueOf(bo.getId()));
        rs.setOrderId(bo.getOrderId());

        if (bo.getOrderItems() != null && !bo.getOrderItems().isEmpty()) {
            rs.setOrderedItems(
                    bo.getOrderItems() == null
                            ? Collections.emptyList()
                            : mapToOrderItemRsList(bo.getOrderItems())
            );

        }

        rs.setTotalAmount(bo.getTotalAmount());
        rs.setOrderTime(bo.getOrderTime());
        rs.setPaymentMethod(bo.getPaymentMethod());
        rs.setPaymentStatus(bo.getPaymentStatus());
        rs.setOrderStatus(bo.getOrderStatus());

        rs.setUser(userMapper.mapToUserRs(bo.getUser()));

        return rs;
    }

    public List<OrderRs> mapToOrderRsList(List<OrderBO> bos) {

        if (bos == null || bos.isEmpty()) {
            return Collections.emptyList();
        }

        return bos.stream()
                .map(this::mapToOrderRs)
                .toList();
    }


    public static OrderSummaryRs toOrderSummary(
            OrderBO order,
            FileService fileService
    ) {

        List<OrderItemRs> items = order.getOrderItems().stream().map(item -> {

            String imageUrl = null;
            if (item.getProduct().getImageFileIds() != null &&
                    !item.getProduct().getImageFileIds().isEmpty()) {

                imageUrl = fileService.getPublicFileUrl(
                        item.getProduct().getImageFileIds().get(0)
                );
            }

            return OrderItemRs.builder()
                    .productId(item.getProduct().getId())
                    .productName(item.getProductName())
                    .unitPrice(item.getUnitPrice())
                    .quantity(item.getQuantity())
                    .totalPrice(item.getTotalPrice())
                    .imageUrl(imageUrl)
                    .build();
        }).toList();

        return OrderSummaryRs.builder()
                .orderId(order.getOrderId())
                .subTotal(order.getSubTotal())
                .taxAmount(order.getTaxAmount())
                .deliveryCharge(order.getDeliveryCharge())
                .discountAmount(order.getDiscountAmount())
                .totalAmount(order.getTotalAmount())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .orderStatus(order.getOrderStatus())
                .orderTime(order.getOrderTime())
                .items(items)
                .build();
    }

    public  List<OrderItemRs> mapToOrderItemRsList(
            List<OrderItemBO> items) {

        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }

        List<OrderItemRs> list = new ArrayList<>();
        for (OrderItemBO item : items) {
            OrderItemRs rs = new OrderItemRs();
            rs.setProductName(item.getProductName());
            rs.setQuantity(item.getQuantity());
            rs.setUnitPrice(item.getUnitPrice());
            rs.setTotalPrice(item.getTotalPrice());
            rs.setSellerId(item.getSellerId());

            // optional image
            if (item.getProduct() != null
                    && item.getProduct().getImageFileIds() != null
                    && !item.getProduct().getImageFileIds().isEmpty()) {

                Long imageId = item.getProduct().getImageFileIds().get(0);
                rs.setImageUrl(urlUtils.publicFile(imageId));
            }


            list.add(rs);
        }
        return list;
    }

}

