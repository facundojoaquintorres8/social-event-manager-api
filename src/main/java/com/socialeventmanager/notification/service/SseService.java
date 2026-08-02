package com.socialeventmanager.notification.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.socialeventmanager.notification.enums.NotificationType;

public interface SseService {

    SseEmitter subscribe(UUID userId);

    void sendToUsers(List<UUID> userIds, UUID eventId,
            NotificationType type, Map<String, String> params);
}