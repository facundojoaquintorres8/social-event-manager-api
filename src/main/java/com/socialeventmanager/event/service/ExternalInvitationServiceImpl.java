package com.socialeventmanager.event.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.socialeventmanager.auth.service.CurrentUserService;
import com.socialeventmanager.event.dto.EventParticipantResponseDTO;
import com.socialeventmanager.event.dto.ExternalInvitationPreviewResponseDTO;
import com.socialeventmanager.event.dto.RemoveInvitationRequestDTO;
import com.socialeventmanager.event.entity.Event;
import com.socialeventmanager.event.entity.ExternalInvitation;
import com.socialeventmanager.event.enums.EventStatus;
import com.socialeventmanager.event.enums.ExternalInvitationStatus;
import com.socialeventmanager.event.repository.EventRepository;
import com.socialeventmanager.event.repository.ExternalInvitationRepository;
import com.socialeventmanager.shared.dto.ApiResponseDTO;
import com.socialeventmanager.shared.exception.BadRequestException;
import com.socialeventmanager.shared.util.EventValidator;
import com.socialeventmanager.user.entity.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExternalInvitationServiceImpl implements ExternalInvitationService {

    private final ExternalInvitationRepository externalInvitationRepository;
    private final InvitationService invitationService;
    private final CurrentUserService currentUserService;
    private final EventRepository eventRepository;
    private final EventValidator eventValidator;

    @Override
    public ApiResponseDTO<Void> inviteExternalUser(
            Event event,
            User currentUser,
            String email) {
        ExternalInvitation existingInvitation = externalInvitationRepository
                .findByEventAndInvitedEmail(event, email)
                .orElse(null);

        if (existingInvitation != null) {

            if (existingInvitation.getStatus() == ExternalInvitationStatus.CANCELLED) {
                existingInvitation.setStatus(ExternalInvitationStatus.PENDING);

                externalInvitationRepository.save(existingInvitation);

                return new ApiResponseDTO<>(
                        true,
                        "User invited successfully",
                        null);
            }

            throw new BadRequestException("User already invited");
        }

        ExternalInvitation invitation = ExternalInvitation.builder()
                .event(event)
                .invitedBy(currentUser)
                .invitedEmail(email)
                .token(UUID.randomUUID().toString())
                .status(ExternalInvitationStatus.PENDING)
                .expiresAt(event.getEventDate())
                .build();

        externalInvitationRepository.save(invitation);

        return new ApiResponseDTO<>(
                true,
                "Invitation sent successfully",
                null);
    }

    @Override
    public ApiResponseDTO<Void> removeExternalInvitation(RemoveInvitationRequestDTO request) {
        User currentUser = getCurrentUser();

        Event event = eventRepository.findByIdAndCreatedBy(request.getEventId(), currentUser)
                .orElseThrow(() -> new BadRequestException("Event not found"));

        eventValidator.validateEventAllowsInteraction(event);

        String email = request.getEmail().trim().toLowerCase();

        ExternalInvitation invitation = externalInvitationRepository
                .findByEventAndInvitedEmail(event, email)
                .orElseThrow(() -> new BadRequestException("Invitation not found"));

        if (invitation.getStatus() == ExternalInvitationStatus.CANCELLED) {
            throw new BadRequestException(
                    "Invitation already cancelled");
        }

        invitation.setStatus(ExternalInvitationStatus.CANCELLED);

        externalInvitationRepository.save(invitation);

        return new ApiResponseDTO<>(
                true,
                "Invitation cancelled successfully",
                null);
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
                .invitedEmail(invitation.getInvitedEmail())
                .build();

        return new ApiResponseDTO<>(
                true,
                "Invitation retrieved successfully",
                response);
    }

    @Override
    public void claimExternalInvitations(User user) {

        List<ExternalInvitation> invitations = externalInvitationRepository
                .findAllByInvitedEmailAndStatus(
                        user.getEmail(),
                        ExternalInvitationStatus.PENDING);

        for (ExternalInvitation externalInvitation : invitations) {
            if (externalInvitation.getEvent().getStatus() == EventStatus.CANCELLED
                    || externalInvitation.getEvent().getEventDate().isBefore(LocalDateTime.now())) {
                continue;
            }

            invitationService.inviteExistingUser(externalInvitation.getEvent(), externalInvitation.getInvitedBy(),
                    user);

            externalInvitation.setStatus(ExternalInvitationStatus.CLAIMED);
            externalInvitation.setClaimedAt(LocalDateTime.now());
            externalInvitationRepository.save(externalInvitation);
        }
    }

    @Override
    public void updateExternalInvitationExpiryDates(Event event) {
        List<ExternalInvitation> invitations = externalInvitationRepository.findAllByEventAndStatus(event,
                ExternalInvitationStatus.PENDING);
        invitations.forEach(invitation -> invitation.setExpiresAt(event.getEventDate()));
        externalInvitationRepository.saveAll(invitations);
    }

    @Override
    public void cancelExternalInvitationsForEvent(Event event) {
        List<ExternalInvitation> externalInvitations = externalInvitationRepository.findAllByEvent(event);
        for (ExternalInvitation invitation : externalInvitations) {
            invitation.setStatus(ExternalInvitationStatus.CANCELLED);
        }
        externalInvitationRepository.saveAll(externalInvitations);
    }

    @Override
    public List<EventParticipantResponseDTO> findAllByEventAndPending(Event event) {

        return externalInvitationRepository
                .findAllByEventAndStatus(event, ExternalInvitationStatus.PENDING)
                .stream()
                .map(invitation -> EventParticipantResponseDTO.builder()
                        .email(invitation.getInvitedEmail())
                        .status(invitation.getStatus().name())
                        .external(true)
                        .build())
                .toList();
    }

    private User getCurrentUser() {
        return currentUserService.getCurrentUser();
    }

    private ExternalInvitation validateExternalInvitation(String token) {

        ExternalInvitation invitation = externalInvitationRepository
                .findByToken(token)
                .orElseThrow(() -> new BadRequestException("Invitation not found"));

        if (invitation.getStatus() == ExternalInvitationStatus.CANCELLED) {
            throw new BadRequestException("Invitation cancelled");
        }

        if (!invitation.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new BadRequestException("Invitation expired");
        }

        if (invitation.getEvent().getStatus() == EventStatus.CANCELLED) {
            throw new BadRequestException("Event cancelled");
        }

        return invitation;
    }

}