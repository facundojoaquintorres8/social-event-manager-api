package com.socialeventmanager.notification.dto;

import com.socialeventmanager.notification.enums.NotificationType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class NotificationResponseDTO {
    private UUID id;
    private UUID eventId;
    private NotificationType type;
    private Map<String, String> params;
    private boolean read;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
}