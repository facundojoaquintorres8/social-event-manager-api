package com.socialeventmanager.notification.service;

import com.socialeventmanager.notification.enums.NotificationType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SseServiceImpl implements SseService {

    private final ConcurrentHashMap<UUID, SseEmitter> emitters = new ConcurrentHashMap<>();

    @Override
    public SseEmitter subscribe(UUID userId) {
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L); // timeout 30 minutes

        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout(() -> emitters.remove(userId));
        emitter.onError(e -> emitters.remove(userId));

        emitters.put(userId, emitter);

        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("connected"));
        } catch (IOException e) {
            emitters.remove(userId);
        }

        return emitter;
    }

    @Override
    public void sendToUsers(List<UUID> userIds, UUID eventId,
            NotificationType type, Map<String, String> params) {
        userIds.forEach(userId -> {
            SseEmitter emitter = emitters.get(userId);
            if (emitter != null) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("notification")
                            .data(Map.of(
                                    "eventId", eventId,
                                    "type", type,
                                    "params", params)));
                } catch (IOException e) {
                    log.error("Failed to send SSE to user {}: {}", userId, e.getMessage());
                    emitters.remove(userId);
                }
            }
        });
    }
}