package com.socialeventmanager.event.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;

import com.socialeventmanager.event.dto.EventParticipantResponseDTO;
import com.socialeventmanager.event.dto.InvitationResponseDTO;
import com.socialeventmanager.event.dto.RemoveInvitationRequestDTO;
import com.socialeventmanager.event.dto.UpdateInvitationStatusRequestDTO;
import com.socialeventmanager.event.entity.Event;
import com.socialeventmanager.event.entity.EventInvitation;
import com.socialeventmanager.event.enums.InvitationStatus;
import com.socialeventmanager.shared.dto.ApiResponseDTO;
import com.socialeventmanager.user.entity.User;

public interface InvitationService {

    List<EventParticipantResponseDTO> findAllByEventAndNotCancelled(Event event);

    ApiResponseDTO<Void> inviteExistingUser(Event event, User invitedBy, User invitedUser);

    Page<EventInvitation> findAllAccepted(
            int page,
            int size,
            String sortBy,
            String direction);

    List<EventInvitation> findAllToCalendarEvents(User currentUser, LocalDateTime from, LocalDateTime to);

    void cancelInvitationsForEvent(Event event);

    ApiResponseDTO<Page<InvitationResponseDTO>> getMyInvitations(
            int page,
            int size,
            String sortBy,
            String direction,
            InvitationStatus status);

    ApiResponseDTO<Void> updateInvitationStatus(
            UUID eventId,
            UpdateInvitationStatusRequestDTO request);

    ApiResponseDTO<Void> removeInvitation(
            UUID eventId,
            RemoveInvitationRequestDTO request);

    boolean existsByEventIdAndInvitedUserAndNotCancelled(UUID eventId, User currentUser);

}