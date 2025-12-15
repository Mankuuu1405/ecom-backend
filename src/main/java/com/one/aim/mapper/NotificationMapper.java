package com.one.aim.mapper;


import com.one.aim.bo.NotificationEventBO;
import com.one.aim.bo.NotificationUserStatusBO;
import com.one.aim.rs.NotificationRS;
import com.one.aim.service.FileService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public class NotificationMapper {

    public static Map<String, Object> map(
            NotificationEventBO event,
            NotificationUserStatusBO status,
            FileService fileService
    ) {

        Map<String, Object> dto = new LinkedHashMap<>();

        dto.put("statusId", status.getId());
        dto.put("eventId", event.getId());
        dto.put("type", event.getType());
        dto.put("title", event.getTitle());
        dto.put("message", event.getDescription());
        dto.put("createdAt", status.getCreatedAt());
        dto.put("isRead", status.getIsRead());


        // ========= IMAGE PUBLIC URL =========
        if (event.getImageFileId() != null) {
            String url = fileService.getPublicFileUrl(event.getImageFileId());
            dto.put("imageUrl", url);
        }


        // ================= WHO TRIGGERED =================
        if (event.getTriggeredByUser() != null) {
            var trigger = event.getTriggeredByUser();
            dto.put("triggerUserId", trigger.getId());
            dto.put("triggerUserName", trigger.getFullName());
        }

        // ================= ORDER DATA =================
        var order = event.getOrder();
        if (order != null) {

            dto.put("orderId", order.getId());
            dto.put("totalAmount", order.getTotalAmount());

            var items = order.getOrderItems();
            if (items != null && !items.isEmpty()) {
                var firstItem = items.get(0);

                // Product
                var product = firstItem.getProduct();
                if (product != null) {
                    dto.put("productId", product.getId());
                    dto.put("productName", product.getName());

                    if (product.getImageFileIds() != null &&
                            !product.getImageFileIds().isEmpty()) {
                        Long imgId = product.getImageFileIds().get(0);
                        dto.put("productImageUrl", fileService.getPublicFileUrl(imgId));
                    }

                    // Seller
                    var seller = product.getSeller();
                    if (seller != null) {
                        dto.put("sellerId", seller.getSellerId()); // UNIQUE STRING ID
                        dto.put("sellerName", seller.getFullName());
                        dto.put("sellerEmail", seller.getEmail());
                    }
                }
            }
        }

        // ================= PRODUCT DIRECT EVENT =================
        var productEvent = event.getProduct();
        if (productEvent != null) {
            dto.put("productId", productEvent.getId());
            dto.put("productName", productEvent.getName());

            if (productEvent.getImageFileIds() != null &&
                    !productEvent.getImageFileIds().isEmpty()) {
                Long imgId = productEvent.getImageFileIds().get(0);
                dto.put("productImageUrl", fileService.getPublicFileUrl(imgId));
            }

            var seller = productEvent.getSeller();
            if (seller != null) {
                dto.put("sellerId", seller.getSellerId());
                dto.put("sellerName", seller.getFullName());
                dto.put("sellerEmail", seller.getEmail());
            }
        }

        // ================= SELLER DIRECT EVENT =================
        var sellerEvent = event.getSeller();
        if (sellerEvent != null) {
            dto.put("sellerId", sellerEvent.getSellerId());
            dto.put("sellerName", sellerEvent.getFullName());
            dto.put("sellerEmail", sellerEvent.getEmail());
        }

        return dto;
    }


    private static String formatTimestamp(LocalDateTime createdAt) {
        Duration diff = Duration.between(createdAt, LocalDateTime.now());
        if (diff.toMinutes() < 1) return "Just now";
        if (diff.toMinutes() < 60) return diff.toMinutes() + " min ago";
        if (diff.toHours() < 24) return diff.toHours() + " hr ago";
        return diff.toDays() + " days ago";
    }
}
