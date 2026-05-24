package com.socialeventmanager.event.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.socialeventmanager.event.dto.ExternalInvitationPreviewResponseDTO;
import com.socialeventmanager.event.entity.Event;
import com.socialeventmanager.event.entity.EventInvitation;
import com.socialeventmanager.event.entity.ExternalInvitation;
import com.socialeventmanager.event.enums.EventStatus;
import com.socialeventmanager.event.enums.ExternalInvitationStatus;
import com.socialeventmanager.event.enums.InvitationStatus;
import com.socialeventmanager.event.repository.EventInvitationRepository;
import com.socialeventmanager.event.repository.ExternalInvitationRepository;
import com.socialeventmanager.shared.dto.ApiResponseDTO;
import com.socialeventmanager.shared.exception.BadRequestException;
import com.socialeventmanager.user.entity.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExternalInvitationServiceImpl implements ExternalInvitationService {

    private final ExternalInvitationRepository externalInvitationRepository;

    private final EventInvitationRepository invitationRepository;

    @Override
    public void claimExternalInvitations(User user) {

        List<ExternalInvitation> invitations = externalInvitationRepository
                .findAllByInvitedEmailAndStatus(
                        user.getEmail(),
                        ExternalInvitationStatus.PENDING);

        for (ExternalInvitation externalInvitation : invitations) {

            boolean alreadyExists = invitationRepository
                    .findByEventAndInvitedUser(
                            externalInvitation.getEvent(),
                            user)
                    .isPresent();

            if (!alreadyExists) {

                EventInvitation invitation = EventInvitation.builder()
                        .event(externalInvitation.getEvent())
                        .invitedUser(user)
                        .invitedBy(externalInvitation.getInvitedBy())
                        .status(InvitationStatus.PENDING)
                        .build();

                invitationRepository.save(invitation);
            }

            externalInvitation.setStatus(ExternalInvitationStatus.CLAIMED);
            externalInvitation.setClaimedAt(LocalDateTime.now());
            externalInvitationRepository.save(externalInvitation);
        }
    }

    @Override
    public ApiResponseDTO<ExternalInvitationPreviewResponseDTO> getInvitationPreview(String token) {

        ExternalInvitation invitation = validateExternalInvitation(token);

        Event event = invitation.getEvent();

        ExternalInvitationPreviewResponseDTO response = ExternalInvitationPreviewResponseDTO.builder()
                .eventId(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .eventDate(event.getEventDate())
                .location(event.getLocation())
                .createdBy(event.getCreatedBy().getEmail())
                .status(invitation.getStatus())
                .expiresAt(invitation.getExpiresAt())
                .alreadyClaimed(invitation.getStatus() == ExternalInvitationStatus.CLAIMED)
                .build();

        return new ApiResponseDTO<>(
                true,
                "Invitation retrieved successfully",
                response);
    }

    private ExternalInvitation validateExternalInvitation(String token) {

        ExternalInvitation invitation = externalInvitationRepository
                .findByToken(token)
                .orElseThrow(() -> new BadRequestException("Invitation not found"));

        if (invitation.getStatus() == ExternalInvitationStatus.CANCELLED) {
            throw new BadRequestException("Invitation cancelled");
        }

        if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Invitation expired");
        }

        if (invitation.getEvent().getStatus() == EventStatus.CANCELLED) {
            throw new BadRequestException("Event cancelled");
        }

        return invitation;
    }
}