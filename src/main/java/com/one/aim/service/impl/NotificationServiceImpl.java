package com.one.aim.service.impl;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.one.aim.bo.*;
import com.one.aim.controller.NotificationWSController;
import com.one.aim.mapper.NotificationMapper;
import com.one.aim.repo.*;
import com.one.aim.service.FileService;
import com.one.vm.core.BaseRs;
import com.one.vm.utils.ResponseUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

//import com.one.aim.bo.NotificationBO;
//import com.one.aim.repo.NotificationRepo;
import com.one.aim.service.NotificationService;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationEventRepo eventRepo;
    private final NotificationUserStatusRepo statusRepo;
    private final UserRepo userRepo;
    private final AdminRepo adminRepo;
    private final SellerRepo sellerRepo;
    private final NotificationWSController wsController;
    private final FileService fileService;
    private final NotificationUserStatusRepo userStatusRepo;

    private static final int MAX_VISIBLE_PER_USER = 20;
    private static final int EXPIRY_DAYS = 10;

    // =====================================================
    //         NOTIFY ADMINS (ENHANCED WITH DETAILS)
    // =====================================================

    @Override
    public void notifyAdmins(
            String type,
            String title,
            String description,
            SellerBO seller,
            ProductBO product,
            OrderBO order,
            String redirectUrl
    ) {
        Long imageFileId = null;

        if (product != null && product.getImageFileIds() != null && !product.getImageFileIds().isEmpty()) {
            imageFileId = product.getImageFileIds().get(0);
        } else if (seller != null) {
            imageFileId = seller.getImageFileId();
        }

        String enhancedDescription = buildEnhancedDescription(
                type, description, seller, product, order
        );

        NotificationEventBO event = NotificationEventBO.builder()
                .type(type)
                .title(title)
                .description(enhancedDescription)
                .order(order)
                .product(product)
                .seller(seller)
                .triggeredByUser(order != null ? order.getUser() : null)
                .imageFileId(imageFileId)
                .redirectUrl(redirectUrl)
                .targetRole("ADMIN")
                .expiryAt(LocalDateTime.now().plusDays(EXPIRY_DAYS))
                .build();

        event = eventRepo.save(event);
        final NotificationEventBO finalEvent = event;

        adminRepo.findAll().forEach(admin -> {
            NotificationUserStatusBO status = saveUserStatus(admin.getId(), finalEvent);
            wsController.sendToUserWS(
                    admin.getId(),
                    NotificationMapper.map(finalEvent, status, fileService)
            );
        });
    }

    // =====================================================
    //         BUILD ENHANCED DESCRIPTION
    // =====================================================

    private String buildEnhancedDescription(
            String type,
            String baseDescription,
            SellerBO seller,
            ProductBO product,
            OrderBO order
    ) {
        StringBuilder desc = new StringBuilder();

        switch (type) {
            case "PRODUCT_ADDED":
                if (seller != null && product != null) {
                    desc.append("🏪 Seller: ").append(seller.getFullName())
                            .append(" (ID: ").append(seller.getId()).append(")")
                            .append("\n📦 Product: ").append(product.getName())
                            .append("\n💰 Price: ₹").append(product.getPrice())
                            .append("\n📊 Stock: ").append(product.getStock())
                            .append("\n🏷️ Category: ").append(product.getCategoryName());
                }
                break;

            case "PRODUCT_UPDATED":
                if (seller != null && product != null) {
                    desc.append("🏪 Seller: ").append(seller.getFullName())
                            .append(" (ID: ").append(seller.getId()).append(")")
                            .append("\n📦 Product: ").append(product.getName())
                            .append("\n\n📝 Changes:\n").append(baseDescription);
                }
                break;

            case "ORDER_PLACED":
                if (order != null) {
                    desc.append("👤 Customer: ").append(order.getUser().getFullName())
                            .append(" (ID: ").append(order.getUser().getId()).append(")")
                            .append("\n🧾 Order ID: ").append(order.getOrderId())  //  Changed from getInvoiceno() to getOrderId()
                            .append("\n💰 Total Amount: ₹").append(order.getTotalAmount())
                            .append("\n💳 Payment: ").append(order.getPaymentMethod())
                            .append("\n📦 Items: ").append(order.getOrderItems() != null ? order.getOrderItems().size() : 0)
                            .append("\n📍 Status: ").append(order.getOrderStatus());
                }
                break;

            case "SELLER_REGISTRATION":
                if (seller != null) {
                    desc.append("🏪 Seller: ").append(seller.getFullName())
                            .append("\n🆔 ID: ").append(seller.getId())
                            .append("\n📧 Email: ").append(seller.getEmail())
                            .append("\n📱 Phone: ").append(seller.getPhoneNo() != null ? seller.getPhoneNo() : "N/A");
//                            .append("\n🏢 Business: ").append(seller.getBusinessName() != null ? seller.getBusinessName() : "N/A");
                }
                break;

            default:
                return baseDescription;
        }

        return desc.toString();
    }

    // =====================================================
    //    NOTIFY SINGLE USER (WITH DETAILS)
    // =====================================================

    @Override
    public void notifyUser(
            Long userId,
            String type,
            String title,
            String description,
            Long imageFileId,
            Long redirectRefId,
            String redirectUrl
    ) {
        NotificationEventBO event = saveEvent(
                type, title, description,
                imageFileId, redirectRefId, redirectUrl,
                null
        );

        NotificationUserStatusBO status = saveUserStatus(userId, event);

        wsController.sendToUserWS(
                userId,
                NotificationMapper.map(event, status, fileService)
        );
    }

    // =====================================================
    //    BROADCAST NOTIFICATIONS (ADMIN SENT)
    // =====================================================

    @Override
    public void notifyAllUsers(
            String type,
            String title,
            String description,
            Long imageFileId,
            Long redirectRefId,
            String redirectUrl
    ) {
        NotificationEventBO event = saveEvent(
                type, title, description,
                imageFileId, redirectRefId, redirectUrl,
                "USER"
        );

        userRepo.findAll().forEach(u -> {
            NotificationUserStatusBO status = saveUserStatus(u.getId(), event);
            wsController.sendToUserWS(
                    u.getId(),
                    NotificationMapper.map(event, status, fileService)
            );
        });
    }

    @Override
    public void notifyAllSellers(
            String type,
            String title,
            String description,
            Long imageFileId,
            Long redirectRefId,
            String redirectUrl
    ) {
        NotificationEventBO event = saveEvent(
                type,
                title,
                description,
                imageFileId,
                redirectRefId,
                redirectUrl,
                "SELLER"
        );

        sellerRepo.findAll().forEach(s -> {
            NotificationUserStatusBO status = saveUserStatus(s.getId(), event);
            wsController.sendToUserWS(
                    s.getId(),
                    NotificationMapper.map(event, status, fileService)
            );
        });
    }

    @Override
    public void notifyBroadcast(String type, String title, String description,
                                Long imageFileId, Long redirectRefId, String redirectUrl) {

        NotificationEventBO event = saveEvent(type, title, description, imageFileId, redirectRefId, redirectUrl, "ALL");

        Set<Long> sentIds = new HashSet<>();  // ✅ prevent duplicates

        userRepo.findAll().forEach(u -> {
            if (sentIds.add(u.getId())) {  // sends only once
                var status = saveUserStatus(u.getId(), event);
                wsController.sendToUserWS(u.getId(), NotificationMapper.map(event, status, fileService));
            }
        });

        sellerRepo.findAll().forEach(s -> {
            if (sentIds.add(s.getId())) {  // sends only if not already sent
                var status = saveUserStatus(s.getId(), event);
                wsController.sendToUserWS(s.getId(), NotificationMapper.map(event, status, fileService));
            }
        });
    }


    // =====================================================
    //     HELPER METHOD: SEND ORDER NOTIFICATIONS (FIXED)
    // =====================================================

    public void sendOrderNotifications(OrderBO order) {
        // Notify all admins about new order
        notifyAdmins(
                "ORDER_PLACED",
                "New Order Placed",
                "",
                null,
                null,
                order,
                "/admin/orders/" + order.getOrderId()
        );

        // Notify each seller whose products are in the order
        if (order.getOrderItems() != null && !order.getOrderItems().isEmpty()) {
            Map<Long, List<OrderItemBO>> sellerItems = order.getOrderItems().stream()
                    .collect(Collectors.groupingBy(OrderItemBO::getSellerId));

            sellerItems.forEach((sellerId, items) -> {
                double sellerTotal = items.stream()
                        .mapToDouble(OrderItemBO::getTotalPrice)
                        .sum();

                String sellerDescription = String.format(
                        "👤 Customer: %s\n🧾 Order: %s\n💰 Your Earnings: ₹%.2f\n📦 Items: %d",
                        order.getUser().getFullName(),
                        order.getInvoiceno(),
                        sellerTotal,
                        items.size()
                );

                notifyUser(
                        sellerId,
                        "NEW_ORDER",
                        "New Order Received",
                        sellerDescription,
                        null,
                        null,  // redirectRefId - pass null since orderId is String
                        "/seller/orders/" + order.getOrderId()
                );
            });
        }
    }

    // =====================================================
    //  HELPER METHOD: SEND PRODUCT UPDATE NOTIFICATIONS
    // =====================================================

    public void sendProductUpdateNotification(ProductBO product, Map<String, String> changes) {
        if (changes.isEmpty()) return;

        StringBuilder changesList = new StringBuilder();
        changes.forEach((field, change) ->
                changesList.append("• ").append(field).append(": ").append(change).append("\n")
        );

        notifyAdmins(
                "PRODUCT_UPDATED",
                "Product Updated",
                changesList.toString().trim(),
                product.getSeller(),
                product,
                null,
                "/admin/products/" + product.getSlug()
        );
    }

    // =====================================================
    //                 FETCH NOTIFICATIONS
    // =====================================================

    @Override
    public List<NotificationUserStatusBO> getUnreadForUser(Long userId) {
        return statusRepo.findByUserIdAndIsReadFalseAndIsHiddenFalseOrderByCreatedAtDesc(userId);
    }

    @Override
    public List<NotificationUserStatusBO> getAllForUser(Long userId) {
        return statusRepo.findByUserIdAndIsHiddenFalseOrderByCreatedAtDesc(userId);
    }

    // =====================================================
    //                    MARK AS READ
    // =====================================================

    @Override
    @Transactional
    public void markAsRead(Long statusId) {
        statusRepo.findById(statusId).ifPresent(s -> {
            s.setIsRead(true);
            s.setReadAt(LocalDateTime.now());
        });
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        List<NotificationUserStatusBO> list =
                statusRepo.findByUserIdAndIsReadFalseAndIsHiddenFalseOrderByCreatedAtDesc(userId);

        list.forEach(s -> {
            s.setIsRead(true);
            s.setReadAt(LocalDateTime.now());
        });
        statusRepo.saveAll(list);
    }

    @Override
    @Transactional
    public void hideNotification(Long statusId, Long userId) {
        var status = statusRepo.findById(statusId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        if (!status.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized delete attempt");
        }

        status.setIsHidden(true);
    }

//    @Override
//    public void notifySeller(String receiverId, String orderId, String title, String description, String redirectUrl) {
//
//        Long sellerPkId = Long.valueOf(receiverId); // converting back to SellerBO.id
//
//        userStatusRepo.save(
//                NotificationUserStatusBO.builder()
//                        .userId(sellerPkId)  // storing seller primary key as receiver reference
//                        .event(null)
//                        .isRead(false)
//                        .isHidden(false)
//                        .build()
//        );
//
//        log.info("Notification stored for seller PK ID {}", sellerPkId);
//    }


    @Override
    public BaseRs getMyNotifications(
            Long userId,
            String role,
            int page,
            int size,
            Boolean unread,
            String type
    ) {
        Pageable pageable = PageRequest.of(page, size);

        Page<NotificationUserStatusBO> pageData =
                statusRepo.findFiltered(userId, unread, type, pageable);

        List<Map<String, Object>> list = pageData.getContent().stream()
                .filter(n -> isRoleAllowed(n.getEvent(), role))
                .map(n -> NotificationMapper.map(n.getEvent(), n, fileService))
                .toList();

        return ResponseUtils.success(Map.of(
                "items", list,
                "page", pageData.getNumber(),
                "size", pageData.getSize(),
                "totalPages", pageData.getTotalPages(),
                "totalElements", pageData.getTotalElements(),
                "unreadCount",
                statusRepo.countByUserIdAndIsReadFalseAndIsHiddenFalse(userId)
        ));
    }

    // =====================================================
    //          SAVE EVENT + STATUS + RETENTION
    // =====================================================

    private NotificationEventBO saveEvent(
            String type,
            String title,
            String description,
            Long imageFileId,
            Long redirectRefId,
            String redirectUrl,
            String targetRole
    ) {
        NotificationEventBO event = NotificationEventBO.builder()
                .type(type)
                .title(title)
                .description(description)
                .imageFileId(imageFileId)
                .redirectRefId(redirectRefId)
                .redirectUrl(redirectUrl)
                .targetRole(targetRole)
                .expiryAt(LocalDateTime.now().plusDays(EXPIRY_DAYS))
                .build();

        return eventRepo.save(event);
    }

    private NotificationUserStatusBO saveUserStatus(Long userId, NotificationEventBO event) {
        NotificationUserStatusBO status = NotificationUserStatusBO.builder()
                .userId(userId)
                .event(event)
                .build();

        NotificationUserStatusBO saved = statusRepo.save(status);
        enforceUserRetention(userId);
        return saved;
    }

    private void enforceUserRetention(Long userId) {
        List<NotificationUserStatusBO> all =
                statusRepo.findByUserIdAndIsHiddenFalseOrderByCreatedAtDesc(userId);

        if (all.size() > MAX_VISIBLE_PER_USER) {
            all.subList(MAX_VISIBLE_PER_USER, all.size())
                    .forEach(n -> n.setIsHidden(true));
        }
    }

    private boolean isRoleAllowed(NotificationEventBO event, String role) {
        String target = event.getTargetRole();
        return target == null || "ALL".equals(target) || target.equals(role);
    }
}