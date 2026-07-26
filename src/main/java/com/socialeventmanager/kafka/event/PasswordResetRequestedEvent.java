package com.socialeventmanager.kafka.event;

public record PasswordResetRequestedEvent(
        String email,
        String firstName,
        String resetToken,
        String language) {
}
