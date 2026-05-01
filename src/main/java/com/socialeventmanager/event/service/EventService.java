package com.socialeventmanager.event.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.domain.Page;

import com.socialeventmanager.event.dto.CreateEventRequestDTO;
import com.socialeventmanager.event.dto.EventParticipantResponseDTO;
import com.socialeventmanager.event.dto.EventResponseDTO;
import com.socialeventmanager.event.dto.InvitationResponseDTO;
import com.socialeventmanager.event.dto.InviteUserRequestDTO;
import com.socialeventmanager.event.dto.RemoveInvitationRequestDTO;
import com.socialeventmanager.event.dto.UpdateInvitationStatusRequestDTO;
import com.socialeventmanager.event.enums.EventStatus;
import com.socialeventmanager.event.enums.InvitationStatus;
import com.socialeventmanager.shared.dto.ApiResponseDTO;

public interface EventService {

    ApiResponseDTO<EventResponseDTO> createEvent(CreateEventRequestDTO request);

    ApiResponseDTO<Page<EventResponseDTO>> getMyEvents(
                    int page,
                    int size,
                    String sortBy,
                    String direction,
                    String title,
                    LocalDateTime fromDate,
                    LocalDateTime toDate,
                    EventStatus status);

    ApiResponseDTO<EventResponseDTO> getEventById(UUID eventId);

    ApiResponseDTO<EventResponseDTO> updateEvent(
            UUID eventId,
            CreateEventRequestDTO request);

    ApiResponseDTO<Void> deleteEvent(UUID eventId);

    ApiResponseDTO<Void> inviteUser(
            UUID eventId,
            InviteUserRequestDTO request);

    ApiResponseDTO<Page<InvitationResponseDTO>> getMyInvitations(
                    int page,
                    int size,
                    String sortBy,
                    String direction,
                    InvitationStatus status);

    ApiResponseDTO<Void> updateInvitationStatus(
                    UUID invitationId,
                    UpdateInvitationStatusRequestDTO request);

    ApiResponseDTO<Page<EventParticipantResponseDTO>> getEventParticipants(
                    UUID eventId,
                    int page,
                    int size,
                    String sortBy,
                    String direction);

    ApiResponseDTO<Page<EventResponseDTO>> getAttendingEvents(
                    int page,
                    int size,
                    String sortBy,
                    String direction);

    ApiResponseDTO<Void> removeInvitation(
                    UUID eventId,
                    RemoveInvitationRequestDTO request);
}