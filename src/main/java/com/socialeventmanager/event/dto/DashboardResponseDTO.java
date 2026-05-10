package com.socialeventmanager.event.dto;

import java.util.List;

public record DashboardResponseDTO(
        long totalEvents,
        long activeEvents,
        long cancelledEvents,
        long upcomingEvents,
        List<EventResponseDTO> recentEvents) {
}
