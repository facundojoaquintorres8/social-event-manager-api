package com.socialeventmanager.event.service;

import java.util.List;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.socialeventmanager.event.dto.CreateEventRequestDTO;
import com.socialeventmanager.event.dto.EventResponseDTO;
import com.socialeventmanager.event.entity.Event;
import com.socialeventmanager.event.repository.EventRepository;
import com.socialeventmanager.shared.dto.ApiResponseDTO;
import com.socialeventmanager.shared.exception.BadRequestException;
import com.socialeventmanager.user.entity.User;
import com.socialeventmanager.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    @Override
    public ApiResponseDTO<EventResponseDTO> createEvent(
            CreateEventRequestDTO request) {
        User currentUser = getCurrentUser();

        Event event = Event.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .eventDate(request.getEventDate())
                .location(request.getLocation())
                .createdBy(currentUser)
                .build();

        eventRepository.save(event);

        return new ApiResponseDTO<>(
                true,
                "Event created successfully",
                mapToResponse(event));
    }

    @Override
    public ApiResponseDTO<List<EventResponseDTO>> getMyEvents() {
        User currentUser = getCurrentUser();

        List<EventResponseDTO> events = eventRepository
                .findAllByCreatedBy(currentUser)
                .stream()
                .map(this::mapToResponse)
                .toList();

        return new ApiResponseDTO<>(
                true,
                "Events retrieved successfully",
                events);
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found"));
    }

    private EventResponseDTO mapToResponse(Event event) {
        return EventResponseDTO.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .eventDate(event.getEventDate())
                .location(event.getLocation())
                .createdBy(event.getCreatedBy().getEmail())
                .build();
    }
}