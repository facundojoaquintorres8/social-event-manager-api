package com.socialeventmanager.kafka.event;

import com.socialeventmanager.event.enums.InvitationStatus;
import java.util.UUID;

public record InvitationRespondedEvent(
        UUID invitationId,
        UUID eventId,
        String eventTitle,
        String participantName,
        String organizerEmail,
        InvitationStatus status,
        String language) {
}