package com.socialeventmanager.event.controller;

import com.socialeventmanager.event.dto.*;
import com.socialeventmanager.event.enums.EventStatus;
import com.socialeventmanager.event.enums.InvitationStatus;
import com.socialeventmanager.event.service.EventService;
import com.socialeventmanager.shared.dto.ApiResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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
            @RequestParam(defaultValue = "10") int size,
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

    @GetMapping("/invitations")
    public ResponseEntity<ApiResponseDTO<Page<InvitationResponseDTO>>> getMyInvitations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) InvitationStatus status) {
        return ResponseEntity.ok(
                eventService.getMyInvitations(
                        page,
                        size,
                        sortBy,
                        direction,
                        status));
    }

    @PutMapping("/{eventId}/invitations")
    public ResponseEntity<ApiResponseDTO<Void>> updateInvitationStatus(
            @PathVariable UUID eventId,
            @Valid @RequestBody UpdateInvitationStatusRequestDTO request) {
        return ResponseEntity.ok(
                eventService.updateInvitationStatus(
                        eventId,
                        request));
    }

    @GetMapping("/attending")
    public ResponseEntity<ApiResponseDTO<Page<EventResponseDTO>>> getAttendingEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "eventDate") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        return ResponseEntity.ok(eventService.getAttendingEvents(page, size, sortBy, direction));
    }

    @DeleteMapping("/{eventId}/invite")
    public ResponseEntity<ApiResponseDTO<Void>> removeInvitation(
            @PathVariable UUID eventId,
            @Valid @RequestBody RemoveInvitationRequestDTO request) {
        return ResponseEntity.ok(eventService.removeInvitation(eventId, request));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponseDTO<DashboardResponseDTO>> getDashboard() {
        return ResponseEntity.ok(eventService.getDashboard());
    }

}