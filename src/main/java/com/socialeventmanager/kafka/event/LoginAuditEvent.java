package com.socialeventmanager.kafka.event;

import java.util.UUID;

public record LoginAuditEvent(
        UUID userId,
        String email,
        String ipAddress,
        String userAgent,
        boolean success,
        String failureReason) {
}