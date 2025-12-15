//package com.one.aim.mapper;
//
//import com.one.aim.bo.OrderItemBO;
//import com.one.aim.rs.OrderItemRs;
//import com.one.aim.service.FileService;
//
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.List;
//
//public class OrderItemMapper {
//
//    public static List<OrderItemRs> mapToOrderItemRsList(
//            List<OrderItemBO> items,
//            FileService fileService) {
//
//        if (items == null || items.isEmpty()) {
//            return Collections.emptyList();
//        }
//
//        List<OrderItemRs> list = new ArrayList<>();
//        for (OrderItemBO item : items) {
//            OrderItemRs rs = new OrderItemRs();
//            rs.setProductName(item.getProductName());
//            rs.setQuantity(item.getQuantity());
//            rs.setUnitPrice(item.getUnitPrice());
//            rs.setTotalPrice(item.getTotalPrice());
//            rs.setSellerId(item.getSellerId());
//
//            // optional image
//            if (item.getProduct() != null
//                    && item.getProduct().getImageFileIds() != null
//                    && !item.getProduct().getImageFileIds().isEmpty()) {
//
//                Long imageId = item.getProduct().getImageFileIds().get(0);
//                rs.setImageUrl(fileService.getPublicFileUrl(imageId));
//            }
//
//
//            list.add(rs);
//        }
//        return list;
//    }
//}
//
