package com.socialeventmanager.event.service;

import java.util.List;
import java.util.UUID;

import com.socialeventmanager.event.dto.CreateEventRequestDTO;
import com.socialeventmanager.event.dto.EventResponseDTO;
import com.socialeventmanager.shared.dto.ApiResponseDTO;

public interface EventService {

    ApiResponseDTO<EventResponseDTO> createEvent(CreateEventRequestDTO request);

    ApiResponseDTO<List<EventResponseDTO>> getMyEvents();

    ApiResponseDTO<EventResponseDTO> getEventById(UUID eventId);

    ApiResponseDTO<EventResponseDTO> updateEvent(
            UUID eventId,
            CreateEventRequestDTO request);

    ApiResponseDTO<Void> deleteEvent(UUID eventId);
}