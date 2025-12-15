package com.one.aim.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.one.aim.bo.*;
import com.one.aim.controller.NotificationWSController;
import com.one.aim.mapper.NotificationMapper;
import com.one.aim.repo.*;
import com.one.aim.service.FileService;
import com.one.vm.core.BaseRs;
import com.one.vm.utils.ResponseUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

//import com.one.aim.bo.NotificationBO;
//import com.one.aim.repo.NotificationRepo;
import com.one.aim.service.NotificationService;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;


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

    private static final int MAX_VISIBLE_PER_USER = 20;
    private static final int EXPIRY_DAYS = 10;

    // =====================================================
    //                 SEND NOTIFICATIONS
    // =====================================================

    @Override
    public void notifyAdmins(
            String type,
            String title,
            String description,
            SellerBO seller,           // when seller registers
            ProductBO product,         // when product created/updated
            OrderBO order,             // when order event occurs
            String redirectUrl         // auto-generated externally
    ) {

        // Auto-select the best image
        Long imageFileId = null;

        if (product != null && product.getImageFileIds() != null && !product.getImageFileIds().isEmpty()) {
            imageFileId = product.getImageFileIds().get(0);
        } else if (seller != null) {
            imageFileId = seller.getImageFileId();
        }

        NotificationEventBO event = NotificationEventBO.builder()
                .type(type)
                .title(title)
                .description(description)
                .order(order)        // for ORDER_PLACED/UPDATED
                .product(product)    // for PRODUCT_ADDED/UPDATED
                .seller(seller)      // for SELLER_REGISTERED
                .triggeredByUser(order != null ? order.getUser() : null) // buyer info
                .imageFileId(imageFileId)
                .redirectUrl(redirectUrl)
                .targetRole("ADMIN")
                .expiryAt(LocalDateTime.now().plusDays(EXPIRY_DAYS))
                .build();

        event = eventRepo.save(event);

// create effectively final reference for lambda
        final NotificationEventBO finalEvent = event;

        adminRepo.findAll().forEach(admin -> {
            NotificationUserStatusBO status = saveUserStatus(admin.getId(), finalEvent);
            wsController.sendToUserWS(
                    admin.getId(),
                    NotificationMapper.map(finalEvent, status, fileService)
            );
        });

    }



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
    public void notifyBroadcast(
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
                "ALL"
        );

        userRepo.findAll().forEach(u -> {
            NotificationUserStatusBO status = saveUserStatus(u.getId(), event);
            wsController.sendToUserWS(
                    u.getId(),
                    NotificationMapper.map(event, status, fileService)
            );
        });
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

        list.forEach(s -> s.setIsRead(true));

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

        return target == null           // direct user notification
                || "ALL".equals(target) // broadcast
                || target.equals(role); // role-specific
    }

}
