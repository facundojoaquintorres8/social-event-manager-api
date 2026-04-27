package com.socialeventmanager.event.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
    public ResponseEntity<ApiResponseDTO<List<EventResponseDTO>>> getMyEvents() {
        return ResponseEntity.ok(eventService.getMyEvents());
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
    public ResponseEntity<ApiResponseDTO<List<InvitationResponseDTO>>> getMyInvitations() {
        return ResponseEntity.ok(eventService.getMyInvitations());
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
    public ResponseEntity<ApiResponseDTO<List<EventParticipantResponseDTO>>> getEventParticipants(
            @PathVariable UUID eventId) {
        return ResponseEntity.ok(eventService.getEventParticipants(eventId));
    }
    
    @GetMapping("/attending")
    public ResponseEntity<ApiResponseDTO<List<EventResponseDTO>>> getAttendingEvents() {
        return ResponseEntity.ok(eventService.getAttendingEvents());
    }

}