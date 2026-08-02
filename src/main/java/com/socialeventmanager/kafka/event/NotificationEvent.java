package com.socialeventmanager.kafka.event;

import com.socialeventmanager.notification.enums.NotificationType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record NotificationEvent(
        UUID eventId,
        NotificationType type,
        Map<String, String> params,
        List<UUID> recipientIds) {
}