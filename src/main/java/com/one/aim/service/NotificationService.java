package com.one.aim.service;

import com.one.aim.bo.NotificationUserStatusBO;
import com.one.aim.bo.OrderBO;
import com.one.aim.bo.ProductBO;
import com.one.aim.bo.SellerBO;
import com.one.vm.core.BaseRs;

import java.util.List;

//import com.one.aim.bo.NotificationBO;

public interface NotificationService {

    // Notify all Admins (System events)
    void notifyAdmins(
            String type,
            String title,
            String description,
            SellerBO seller,
            ProductBO product,
            OrderBO order,
            String redirectUrl
    );

    // Notify specific user (Order events)
    void notifyUser(
            Long userId,
            String type,
            String title,
            String description,
            Long imageFileId,
            Long redirectRefId,
            String redirectUrl
    );

    // Notify all Users (Public Sale events)
    void notifyAllUsers(
            String type,
            String title,
            String description,
            Long imageFileId,
            Long redirectRefId,
            String redirectUrl
    );

    // Notify all Sellers
    void notifyAllSellers(
            String type,
            String title,
            String description,
            Long imageFileId,
            Long redirectRefId,
            String redirectUrl
    );

    // Notify everyone
    void notifyBroadcast(
            String type,
            String title,
            String description,
            Long imageFileId,
            Long redirectRefId,
            String redirectUrl
    );

    BaseRs getMyNotifications(
            Long userId,
            String role,
            int page,
            int size,
            Boolean unread,
            String type
    );


    // Fetch unread notifications
    List<NotificationUserStatusBO> getUnreadForUser(Long userId);

    // Fetch all notifications
    List<NotificationUserStatusBO> getAllForUser(Long userId);

    void markAsRead(Long statusId);

    void markAllAsRead(Long userId);

    void hideNotification(Long statusId, Long userId);



}
