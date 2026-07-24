package com.socialeventmanager.kafka.event;

import java.util.List;
import java.util.UUID;

public record EventCancelledEvent(
        UUID eventId,
        String eventTitle,
        String eventLocation,
        Double latitude,
        Double longitude,
        String eventDate,
        String organizerName,
        List<String> participantEmails,
        String language) {
}