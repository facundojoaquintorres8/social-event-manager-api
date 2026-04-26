package com.socialeventmanager.event.service;

import java.util.List;

import com.socialeventmanager.event.dto.CreateEventRequestDTO;
import com.socialeventmanager.event.dto.EventResponseDTO;
import com.socialeventmanager.shared.dto.ApiResponseDTO;

public interface EventService {

    ApiResponseDTO<EventResponseDTO> createEvent(CreateEventRequestDTO request);
    ApiResponseDTO<List<EventResponseDTO>> getMyEvents();
}