package com.socialeventmanager.notification.service;

import com.socialeventmanager.notification.dto.NotificationResponseDTO;
import com.socialeventmanager.notification.entity.Notification;
import com.socialeventmanager.shared.dto.ApiResponseDTO;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface NotificationService {
    void createNotifications(UUID eventId, com.socialeventmanager.notification.enums.NotificationType type,
            java.util.Map<String, String> params, List<UUID> recipientIds);

    List<Notification> getUnreadNotifications(UUID userId);

    ApiResponseDTO<Page<NotificationResponseDTO>> getAllNotifications(
            UUID userId, int page, int size);

    ApiResponseDTO<Void> markAsRead(UUID notificationId, UUID userId);

    ApiResponseDTO<Void> markAllAsRead(UUID userId);

    long countUnread(UUID userId);
}