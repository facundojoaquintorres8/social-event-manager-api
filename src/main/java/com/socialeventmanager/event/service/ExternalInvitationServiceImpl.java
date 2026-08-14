package com.socialeventmanager.event.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
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
import com.socialeventmanager.kafka.event.InvitationCreatedEvent;
import com.socialeventmanager.kafka.producer.EventProducer;
import com.socialeventmanager.notification.service.NotificationLogService;
import com.socialeventmanager.shared.dto.ApiResponseDTO;
import com.socialeventmanager.shared.exception.BadRequestException;
import com.socialeventmanager.shared.util.Constants;
import com.socialeventmanager.shared.util.EmailValidator;
import com.socialeventmanager.shared.util.EventValidator;
import com.socialeventmanager.user.entity.User;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExternalInvitationServiceImpl implements ExternalInvitationService {

    private final ExternalInvitationRepository externalInvitationRepository;
    private final InvitationService invitationService;
    private final CurrentUserService currentUserService;
    private final EventRepository eventRepository;
    private final EventValidator eventValidator;
    private final EventProducer eventProducer;
    private final NotificationLogService notificationLogService;

    @Transactional
    @Override
    public ApiResponseDTO<Void> inviteExternalUser(
            Event event,
            User currentUser,
            String email,
            String language) {
        ExternalInvitation existingInvitation = externalInvitationRepository
                .findByEventAndInvitedEmail(event, email)
                .orElse(null);

        if (existingInvitation != null) {

            if (existingInvitation.getStatus() == ExternalInvitationStatus.CANCELLED) {
                existingInvitation.setStatus(ExternalInvitationStatus.PENDING);

                externalInvitationRepository.save(existingInvitation);

                notificationLogService.deleteByInvitationId(existingInvitation.getId());

                publishExternalInvitationCreatedEvent(existingInvitation, language);

                return new ApiResponseDTO<>(
                        true,
                        "User invited successfully",
                        null);
            }

            throw new BadRequestException("userAlreadyInvited");
        }

        ExternalInvitation invitation = ExternalInvitation.builder()
                .event(event)
                .invitedBy(currentUser)
                .invitedEmail(email)
                .token(UUID.randomUUID().toString())
                .status(ExternalInvitationStatus.PENDING)
                .build();

        externalInvitationRepository.save(invitation);

        publishExternalInvitationCreatedEvent(invitation, language);
        return new ApiResponseDTO<>(
                true,
                "Invitation sent successfully",
                null);
    }

    @Override
    public ApiResponseDTO<Void> removeExternalInvitation(RemoveInvitationRequestDTO request) {
        User currentUser = getCurrentUser();

        Event event = eventRepository.findByIdAndCreatedBy(request.getEventId(), currentUser)
                .orElseThrow(() -> new BadRequestException("eventNotFound"));

        eventValidator.validateEventAllowsInteraction(event);

        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        EmailValidator.validateEmail(email);

        ExternalInvitation invitation = externalInvitationRepository
                .findByEventAndInvitedEmail(event, email)
                .orElseThrow(() -> new BadRequestException("invitationNotFound"));

        if (invitation.getStatus() == ExternalInvitationStatus.CANCELLED) {
            throw new BadRequestException("invitationAlreadyCancelled");
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
                .latitude(event.getLatitude())
                .longitude(event.getLongitude())
                .createdBy(event.getCreatedBy().getEmail())
                .status(invitation.getStatus())
                .alreadyClaimed(invitation.getStatus() == ExternalInvitationStatus.CLAIMED)
                .invitedEmail(invitation.getInvitedEmail())
                .build();

        return new ApiResponseDTO<>(
                true,
                "Invitation retrieved successfully",
                response);
    }

    @Override
    public void claimExternalInvitations(User user, String language) {

        List<ExternalInvitation> invitations = externalInvitationRepository
                .findAllByInvitedEmailAndStatus(
                        user.getEmail(),
                        ExternalInvitationStatus.PENDING);

        for (ExternalInvitation externalInvitation : invitations) {
            if (externalInvitation.getEvent().getStatus() == EventStatus.CANCELLED
                    || externalInvitation.getEvent().getEventDate()
                            .isBefore(LocalDateTime.now(Constants.TIMEZONE_ARGENTINA))) {
                continue;
            }

            invitationService.inviteExistingUser(externalInvitation.getEvent(), externalInvitation.getInvitedBy(),
                    user, language);

            externalInvitation.setStatus(ExternalInvitationStatus.CLAIMED);
            externalInvitation.setClaimedAt(LocalDateTime.now(Constants.TIMEZONE_ARGENTINA));
            externalInvitationRepository.save(externalInvitation);
        }
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

    @Override
    public long countActiveByEvent(Event event) {
        return externalInvitationRepository.countByEventAndStatus(event, ExternalInvitationStatus.PENDING);
    }

    private User getCurrentUser() {
        return currentUserService.getCurrentUser();
    }

    private ExternalInvitation validateExternalInvitation(String token) {

        ExternalInvitation invitation = externalInvitationRepository
                .findByToken(token)
                .orElseThrow(() -> new BadRequestException("invitationNotFound"));

        if (invitation.getStatus() == ExternalInvitationStatus.CANCELLED) {
            throw new BadRequestException("invitationCancelled");
        }

        if (!invitation.getEvent().getEventDate().isAfter(LocalDateTime.now(Constants.TIMEZONE_ARGENTINA))) {
            throw new BadRequestException("invitationExpired");
        }

        if (invitation.getEvent().getStatus() == EventStatus.CANCELLED) {
            throw new BadRequestException("eventCancelled");
        }

        return invitation;
    }

    private void publishExternalInvitationCreatedEvent(ExternalInvitation invitation, String language) {
        InvitationCreatedEvent event = new InvitationCreatedEvent(
                invitation.getId(),
                invitation.getEvent().getTitle(),
                invitation.getEvent().getLocation(),
                invitation.getEvent().getLatitude(),
                invitation.getEvent().getLongitude(),
                invitation.getEvent().getEventDate().toString(),
                invitation.getInvitedBy().getFirstName() + " " +
                        invitation.getInvitedBy().getLastName(),
                invitation.getInvitedEmail(),
                true,
                invitation.getToken(),
                language);

        eventProducer.sendInvitationCreated(event);
    }

}