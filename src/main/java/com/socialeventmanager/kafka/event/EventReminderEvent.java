package com.socialeventmanager.kafka.event;

import java.util.List;
import java.util.UUID;

public record EventReminderEvent(
        UUID eventId,
        String eventTitle,
        String eventLocation,
        String eventDate,
        String organizerName,
        String organizerEmail,
        Double latitude,
        Double longitude,
        List<String> participantEmails) {
}