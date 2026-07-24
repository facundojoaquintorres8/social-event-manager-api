package com.socialeventmanager.kafka.event;

import java.util.UUID;

public record InvitationCreatedEvent(
        UUID invitationId,
        String eventTitle,
        String eventLocation,
        Double latitude,
        Double longitude,
        String eventDate,
        String organizerName,
        String invitedEmail,
        boolean external,
        String externalToken,
        String language) {
}