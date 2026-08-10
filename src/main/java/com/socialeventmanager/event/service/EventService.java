package com.socialeventmanager.event.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;

import com.socialeventmanager.event.dto.BalanceRequestDTO;
import com.socialeventmanager.event.dto.BalanceResponseDTO;
import com.socialeventmanager.event.dto.CalendarEventResponseDTO;
import com.socialeventmanager.event.dto.CreateEventRequestDTO;
import com.socialeventmanager.event.dto.DashboardResponseDTO;
import com.socialeventmanager.event.dto.EventDetailsFullResponseDTO;
import com.socialeventmanager.event.dto.EventFilterRequestDTO;
import com.socialeventmanager.event.dto.EventResponseDTO;
import com.socialeventmanager.event.dto.InviteUserRequestDTO;
import com.socialeventmanager.shared.dto.ApiResponseDTO;

public interface EventService {

    ApiResponseDTO<EventResponseDTO> createEvent(CreateEventRequestDTO request);

    ApiResponseDTO<Page<EventResponseDTO>> getMyEvents(EventFilterRequestDTO filterRequest);

    ApiResponseDTO<Page<EventResponseDTO>> getAttendingEvents(
            int page,
            int size,
            String sortBy,
            String direction);

    ApiResponseDTO<List<CalendarEventResponseDTO>> getCalendarEvents(
            LocalDateTime fromDate,
            LocalDateTime toDate);

    ApiResponseDTO<EventResponseDTO> getEventById(UUID eventId);

    ApiResponseDTO<EventDetailsFullResponseDTO> getEventByIdFull(UUID eventId);

    ApiResponseDTO<EventResponseDTO> updateEvent(
            UUID eventId,
            CreateEventRequestDTO request);

    ApiResponseDTO<Void> deleteEvent(UUID eventId, String language);

    ApiResponseDTO<Void> inviteUser(
            UUID eventId,
            InviteUserRequestDTO request,
            String language);

    ApiResponseDTO<DashboardResponseDTO> getDashboard();

    ApiResponseDTO<BalanceResponseDTO> calculateBalance(
            UUID eventId,
            BalanceRequestDTO request);

}