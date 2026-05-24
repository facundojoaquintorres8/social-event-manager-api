package com.socialeventmanager.event.service;

import java.util.List;

import com.socialeventmanager.event.dto.EventParticipantResponseDTO;
import com.socialeventmanager.event.dto.ExternalInvitationPreviewResponseDTO;
import com.socialeventmanager.event.dto.RemoveInvitationRequestDTO;
import com.socialeventmanager.event.entity.Event;
import com.socialeventmanager.shared.dto.ApiResponseDTO;
import com.socialeventmanager.user.entity.User;

public interface ExternalInvitationService {
    List<EventParticipantResponseDTO> findAllByEventAndNotCancelled(Event event);

    ApiResponseDTO<Void> inviteExternalUser(Event event, User currentUser, String email);

    ApiResponseDTO<Void> removeExternalInvitation(RemoveInvitationRequestDTO request);

    ApiResponseDTO<ExternalInvitationPreviewResponseDTO> getInvitationPreview(String token);

    void claimExternalInvitations(User user);

    void updateExternalInvitationExpiryDates(Event event);

    void cancelExternalInvitationsForEvent(Event event);

}