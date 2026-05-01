package com.socialeventmanager.event.controller;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.socialeventmanager.event.dto.CreateEventRequestDTO;
import com.socialeventmanager.event.dto.EventParticipantResponseDTO;
import com.socialeventmanager.event.dto.EventResponseDTO;
import com.socialeventmanager.event.dto.InvitationResponseDTO;
import com.socialeventmanager.event.dto.InviteUserRequestDTO;
import com.socialeventmanager.event.dto.UpdateInvitationStatusRequestDTO;
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
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "eventDate") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) LocalDateTime fromDate,
            @RequestParam(required = false) LocalDateTime toDate) {
        return ResponseEntity.ok(
                eventService.getMyEvents(
                        page,
                        size,
                        sortBy,
                        direction,
                        title,
                        fromDate,
                        toDate));
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

    @DeleteMapping("/{eventId}")
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
            @RequestParam(defaultValue = "desc") String direction
    ) {
        return ResponseEntity.ok(eventService.getMyInvitations(page, size, sortBy, direction));
    }

    @PutMapping("/invitations/{invitationId}")
    public ResponseEntity<ApiResponseDTO<Void>> updateInvitationStatus(
            @PathVariable UUID invitationId,
            @Valid @RequestBody UpdateInvitationStatusRequestDTO request) {
        return ResponseEntity.ok(
                eventService.updateInvitationStatus(
                        invitationId,
                        request));
    }

    @GetMapping("/{eventId}/participants")
    public ResponseEntity<ApiResponseDTO<Page<EventParticipantResponseDTO>>> getEventParticipants(
            @PathVariable UUID eventId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        return ResponseEntity.ok(
                eventService.getEventParticipants(
                        eventId,
                        page,
                        size,
                        sortBy,
                        direction));
    }
    
    @GetMapping("/attending")
    public ResponseEntity<ApiResponseDTO<Page<EventResponseDTO>>> getAttendingEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "eventDate") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        return ResponseEntity.ok(eventService.getAttendingEvents(page, size, sortBy, direction));
    }

}