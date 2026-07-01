package com.socialeventmanager.event.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.socialeventmanager.event.dto.BalanceRequestDTO;
import com.socialeventmanager.event.dto.BalanceResponseDTO;
import com.socialeventmanager.event.dto.CalendarEventResponseDTO;
import com.socialeventmanager.event.dto.CreateEventRequestDTO;
import com.socialeventmanager.event.dto.DashboardResponseDTO;
import com.socialeventmanager.event.dto.EventDetailsFullResponseDTO;
import com.socialeventmanager.event.dto.EventResponseDTO;
import com.socialeventmanager.event.dto.InviteUserRequestDTO;
import com.socialeventmanager.event.enums.EventStatus;
import com.socialeventmanager.event.service.EventService;
import com.socialeventmanager.shared.dto.ApiResponseDTO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @PostMapping
    public ResponseEntity<ApiResponseDTO<EventResponseDTO>> createEvent(
            @Valid @RequestBody CreateEventRequestDTO request) {
        return ResponseEntity.ok(eventService.createEvent(request));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponseDTO<Page<EventResponseDTO>>> getMyEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "eventDate") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) LocalDateTime fromDate,
            @RequestParam(required = false) LocalDateTime toDate,
            @RequestParam(required = false) EventStatus status) {
        return ResponseEntity.ok(
                eventService.getMyEvents(
                        page,
                        size,
                        sortBy,
                        direction,
                        title,
                        fromDate,
                        toDate,
                        status));
    }

    @GetMapping("/attending")
    public ResponseEntity<ApiResponseDTO<Page<EventResponseDTO>>> getAttendingEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "eventDate") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        return ResponseEntity.ok(eventService.getAttendingEvents(page, size, sortBy, direction));
    }

    @GetMapping("/calendar")
    public ResponseEntity<ApiResponseDTO<List<CalendarEventResponseDTO>>> getCalendarEvents(
            @RequestParam(required = false) LocalDateTime from,
            @RequestParam(required = false) LocalDateTime to) {
        return ResponseEntity.ok(eventService.getCalendarEvents(from, to));
    }

    @GetMapping("/{eventId}/full")
    public ResponseEntity<ApiResponseDTO<EventDetailsFullResponseDTO>> getEventByIdFull(
            @PathVariable UUID eventId) {
        return ResponseEntity.ok(eventService.getEventByIdFull(eventId));
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<ApiResponseDTO<EventResponseDTO>> getEventById(
            @PathVariable UUID eventId) {
        return ResponseEntity.ok(eventService.getEventById(eventId));
    }

    @PutMapping("/{eventId}")
    public ResponseEntity<ApiResponseDTO<EventResponseDTO>> updateEvent(
            @PathVariable UUID eventId,
            @Valid @RequestBody CreateEventRequestDTO request) {
        return ResponseEntity.ok(eventService.updateEvent(eventId, request));
    }

    @PutMapping("/{eventId}/cancel")
    public ResponseEntity<ApiResponseDTO<Void>> deleteEvent(
            @PathVariable UUID eventId) {
        return ResponseEntity.ok(eventService.deleteEvent(eventId));
    }

    @PostMapping("/{eventId}/invite")
    public ResponseEntity<ApiResponseDTO<Void>> inviteUser(
            @PathVariable UUID eventId,
            @Valid @RequestBody InviteUserRequestDTO request) {
        return ResponseEntity.ok(eventService.inviteUser(eventId, request));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponseDTO<DashboardResponseDTO>> getDashboard() {
        return ResponseEntity.ok(eventService.getDashboard());
    }

    @PostMapping("/{eventId}/balance")
    public ResponseEntity<ApiResponseDTO<BalanceResponseDTO>> calculateBalance(
            @PathVariable UUID eventId,
            @Valid @RequestBody BalanceRequestDTO request) {
        return ResponseEntity.ok(eventService.calculateBalance(eventId, request));
    }

}