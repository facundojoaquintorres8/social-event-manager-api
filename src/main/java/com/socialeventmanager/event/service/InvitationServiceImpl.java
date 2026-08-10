package com.socialeventmanager.event.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.socialeventmanager.auth.service.CurrentUserService;
import com.socialeventmanager.event.dto.EventParticipantResponseDTO;
import com.socialeventmanager.event.dto.InvitationResponseDTO;
import com.socialeventmanager.event.dto.RemoveInvitationRequestDTO;
import com.socialeventmanager.event.dto.UpdateInvitationStatusRequestDTO;
import com.socialeventmanager.event.entity.Event;
import com.socialeventmanager.event.entity.EventInvitation;
import com.socialeventmanager.event.enums.InvitationStatus;
import com.socialeventmanager.event.repository.EventRepository;
import com.socialeventmanager.event.repository.InvitationRepository;
import com.socialeventmanager.event.repository.InvitationSpecification;
import com.socialeventmanager.kafka.event.InvitationCreatedEvent;
import com.socialeventmanager.kafka.event.InvitationRespondedEvent;
import com.socialeventmanager.kafka.event.NotificationEvent;
import com.socialeventmanager.kafka.producer.EventProducer;
import com.socialeventmanager.notification.enums.NotificationType;
import com.socialeventmanager.notification.service.NotificationLogService;
import com.socialeventmanager.shared.dto.ApiResponseDTO;
import com.socialeventmanager.shared.exception.BadRequestException;
import com.socialeventmanager.shared.util.Constants;
import com.socialeventmanager.shared.util.EmailValidator;
import com.socialeventmanager.shared.util.EventValidator;
import com.socialeventmanager.user.entity.User;
import com.socialeventmanager.user.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InvitationServiceImpl implements InvitationService {

    private static final String EVENT_TITLE = "eventTitle";

    private final InvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final EventRepository eventRepository;
    private final EventValidator eventValidator;
    private final EventProducer eventProducer;
    private final NotificationLogService notificationLogService;

    @Override
    public List<EventParticipantResponseDTO> findAllByEventAndNotCancelled(Event event) {
        return invitationRepository
                .findAllByEventAndStatusNot(event, InvitationStatus.CANCELLED)
                .stream()
                .map(invitation -> EventParticipantResponseDTO
                        .builder()
                        .userId(invitation.getInvitedUser().getId())
                        .firstName(invitation.getInvitedUser().getFirstName())
                        .lastName(invitation.getInvitedUser().getLastName())
                        .email(invitation.getInvitedUser().getEmail())
                        .status(invitation.getStatus().name())
                        .external(false)
                        .build())
                .toList();
    }

    @Transactional
    @Override
    public ApiResponseDTO<Void> inviteExistingUser(
            Event event,
            User invitedBy,
            User invitedUser,
            String language) {
        EventInvitation existingInvitation = invitationRepository
                .findByEventAndInvitedUser(event, invitedUser)
                .orElse(null);

        if (existingInvitation != null) {

            if (existingInvitation.getStatus() == InvitationStatus.CANCELLED
                    || existingInvitation.getStatus() == InvitationStatus.REJECTED) {
                existingInvitation.setStatus(InvitationStatus.PENDING);

                invitationRepository.save(existingInvitation);

                notificationLogService.deleteByInvitationId(existingInvitation.getId());

                publishInvitationCreatedEvent(existingInvitation, language);

                eventProducer.sendNotification(new NotificationEvent(
                        event.getId(),
                        NotificationType.INVITATION_RECEIVED,
                        Map.of(EVENT_TITLE, event.getTitle()),
                        List.of(invitedUser.getId())));

                return new ApiResponseDTO<>(
                        true,
                        "User invited successfully",
                        null);
            }

            throw new BadRequestException("userAlreadyInvited");
        }

        EventInvitation invitation = EventInvitation.builder()
                .event(event)
                .invitedUser(invitedUser)
                .invitedBy(invitedBy)
                .status(InvitationStatus.PENDING)
                .build();

        invitationRepository.save(invitation);

        publishInvitationCreatedEvent(invitation, language);

        eventProducer.sendNotification(new NotificationEvent(
                event.getId(),
                NotificationType.INVITATION_RECEIVED,
                Map.of(EVENT_TITLE, event.getTitle()),
                List.of(invitedUser.getId())));

        return new ApiResponseDTO<>(
                true,
                "User invited successfully",
                null);
    }

    @Override
    public Page<EventInvitation> findAllAccepted(
            int page,
            int size,
            String sortBy,
            String direction) {

        User currentUser = getCurrentUser();

        size = Math.min(size, Constants.DEFAULT_PAGE_SIZE);

        List<String> allowedSortFields = List.of(
                "eventDate",
                "title",
                "location");

        if (!allowedSortFields.contains(sortBy)) {
            throw new BadRequestException("invalidSortField");
        }

        Sort sort = direction.equalsIgnoreCase("asc")
                ? Sort.by("event." + sortBy).ascending()
                : Sort.by("event." + sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        return invitationRepository
                .findAllByInvitedUserAndStatus(
                        currentUser,
                        InvitationStatus.ACCEPTED,
                        pageable);
    }

    @Override
    public List<EventInvitation> findAllToCalendarEvents(
            User currentUser,
            LocalDateTime from,
            LocalDateTime to) {
        return invitationRepository
                .findAllByInvitedUserAndStatusInAndEvent_EventDateBetween(
                        currentUser,
                        List.of(
                                InvitationStatus.PENDING,
                                InvitationStatus.ACCEPTED,
                                InvitationStatus.REJECTED),
                        from,
                        to);
    }

    @Override
    public void cancelInvitationsForEvent(Event event) {
        List<EventInvitation> invitations = invitationRepository.findAllByEvent(event);
        for (EventInvitation invitation : invitations) {
            invitation.setStatus(InvitationStatus.CANCELLED);
        }
        invitationRepository.saveAll(invitations);
    }

    @Override
    public ApiResponseDTO<Page<InvitationResponseDTO>> getMyInvitations(
            int page,
            int size,
            String sortBy,
            String direction,
            InvitationStatus status) {
        User currentUser = getCurrentUser();

        size = Math.min(size, Constants.DEFAULT_PAGE_SIZE);

        List<String> allowedSortFields = List.of(
                "status",
                "createdAt");

        if (!allowedSortFields.contains(sortBy)) {
            throw new BadRequestException("invalidSortField");
        }

        Sort sort = direction.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<EventInvitation> spec = Specification.<EventInvitation>unrestricted()
                .and(InvitationSpecification.hasUser(currentUser));

        if (status != null) {
            spec = spec.and(InvitationSpecification.hasStatus(status));
        } else {
            spec = spec.and(InvitationSpecification.hasNotStatus(InvitationStatus.CANCELLED));
        }

        Page<InvitationResponseDTO> invitations = invitationRepository
                .findAll(spec, pageable)
                .map(invitation -> InvitationResponseDTO.builder()
                        .invitationId(invitation.getId())
                        .eventId(invitation.getEvent().getId())
                        .title(invitation.getEvent().getTitle())
                        .eventDate(invitation.getEvent().getEventDate())
                        .location(invitation.getEvent().getLocation())
                        .locationAddress(invitation.getEvent().getLocationAddress())
                        .placeId(invitation.getEvent().getPlaceId())
                        .latitude(invitation.getEvent().getLatitude())
                        .longitude(invitation.getEvent().getLongitude())
                        .createdBy(invitation.getInvitedBy().getFirstName() + " " +
                                invitation.getInvitedBy().getLastName())
                        .status(invitation.getStatus())
                        .eventStatus(invitation.getEvent().getStatus())
                        .build());

        return new ApiResponseDTO<>(
                true,
                "Invitations retrieved successfully",
                invitations);
    }

    @Override
    public ApiResponseDTO<Void> updateInvitationStatus(UpdateInvitationStatusRequestDTO request, String language) {
        User currentUser = getCurrentUser();

        if (request.getStatus() == InvitationStatus.PENDING) {
            throw new BadRequestException("invalidInvitationStatus");
        }

        EventInvitation invitation = invitationRepository
                .findByEventIdAndInvitedUser(request.getEventId(), currentUser)
                .orElseThrow(() -> new BadRequestException("invitationNotFound"));

        eventValidator.validateEventAllowsInteraction(invitation.getEvent());

        if (invitation.getStatus() == InvitationStatus.CANCELLED) {
            throw new BadRequestException("invitationAlreadyCancelled");
        }

        if (invitation.getStatus() == request.getStatus()) {
            throw new BadRequestException("invitationAlreadyHasStatus");
        }

        invitation.setStatus(request.getStatus());

        invitationRepository.save(invitation);

        eventProducer.sendInvitationResponded(new InvitationRespondedEvent(
                invitation.getId(),
                invitation.getEvent().getId(),
                invitation.getEvent().getTitle(),
                invitation.getInvitedUser().getFirstName() + " " +
                        invitation.getInvitedUser().getLastName(),
                invitation.getEvent().getCreatedBy().getEmail(),
                request.getStatus(),
                language));

        NotificationType type = request.getStatus() == InvitationStatus.ACCEPTED
                ? NotificationType.INVITATION_ACCEPTED
                : NotificationType.INVITATION_REJECTED;

        eventProducer.sendNotification(new NotificationEvent(
                invitation.getEvent().getId(),
                type,
                Map.of(
                        EVENT_TITLE, invitation.getEvent().getTitle(),
                        "participantName", invitation.getInvitedUser().getFirstName() + " " +
                                invitation.getInvitedUser().getLastName()),
                List.of(invitation.getEvent().getCreatedBy().getId())));

        return new ApiResponseDTO<>(
                true,
                "Invitation updated successfully",
                null);
    }

    @Override
    public ApiResponseDTO<Void> removeInvitation(RemoveInvitationRequestDTO request) {
        User currentUser = getCurrentUser();

        Event event = eventRepository.findByIdAndCreatedBy(request.getEventId(), currentUser)
                .orElseThrow(() -> new BadRequestException("eventNotFound"));

        eventValidator.validateEventAllowsInteraction(event);

        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        EmailValidator.validateEmail(email);

        Optional<User> invitedUserOptional = userRepository.findByEmail(email);

        if (invitedUserOptional.isPresent()) {
            return removeExistingInvitation(event, invitedUserOptional.get());
        }

        throw new BadRequestException("invitedUserNotFound");
    }

    @Override
    public boolean existsByEventIdAndInvitedUserAndNotCancelled(
            UUID eventId,
            User currentUser) {
        return invitationRepository.existsByEventIdAndInvitedUserAndStatusNot(
                eventId,
                currentUser,
                InvitationStatus.CANCELLED);
    }

    @Override
    public List<InvitationResponseDTO> getRecentInvitations(User user) {
        return invitationRepository
                .findTop5ByInvitedUserIdAndStatusNotOrderByCreatedAtDesc(
                        user.getId(),
                        InvitationStatus.CANCELLED)
                .stream()
                .map(inv -> InvitationResponseDTO.builder()
                        .invitationId(inv.getId())
                        .eventId(inv.getEvent().getId())
                        .title(inv.getEvent().getTitle())
                        .eventDate(inv.getEvent().getEventDate())
                        .location(inv.getEvent().getLocation())
                        .locationAddress(inv.getEvent().getLocationAddress())
                        .placeId(inv.getEvent().getPlaceId())
                        .latitude(inv.getEvent().getLatitude())
                        .longitude(inv.getEvent().getLongitude())
                        .createdBy(inv.getInvitedBy().getFirstName() + " " +
                                inv.getInvitedBy().getLastName())
                        .status(inv.getStatus())
                        .eventStatus(inv.getEvent().getStatus())
                        .build())
                .toList();
    }

    @Override
    public List<String> getAcceptedParticipantEmails(Event event) {
        return invitationRepository
                .findAllByEventAndStatus(event, InvitationStatus.ACCEPTED)
                .stream()
                .map(inv -> inv.getInvitedUser().getEmail())
                .toList();
    }

    @Override
    public List<UUID> getAcceptedParticipantIds(Event event) {
        return invitationRepository
                .findAllByEventAndStatus(event, InvitationStatus.ACCEPTED)
                .stream()
                .map(inv -> inv.getInvitedUser().getId())
                .toList();
    }

    private User getCurrentUser() {
        return currentUserService.getCurrentUser();
    }

    private ApiResponseDTO<Void> removeExistingInvitation(
            Event event,
            User invitedUser) {
        EventInvitation invitation = invitationRepository
                .findByEventAndInvitedUser(
                        event,
                        invitedUser)
                .orElseThrow(() -> new BadRequestException("invitationNotFound"));

        if (invitation.getStatus() == InvitationStatus.CANCELLED) {
            throw new BadRequestException("invitationAlreadyCancelled");
        }

        invitation.setStatus(InvitationStatus.CANCELLED);

        invitationRepository.save(invitation);

        return new ApiResponseDTO<>(
                true,
                "Invitation cancelled successfully",
                null);
    }

    private void publishInvitationCreatedEvent(EventInvitation invitation, String language) {
        InvitationCreatedEvent event = new InvitationCreatedEvent(
                invitation.getId(),
                invitation.getEvent().getTitle(),
                invitation.getEvent().getLocation(),
                invitation.getEvent().getLatitude(),
                invitation.getEvent().getLongitude(),
                invitation.getEvent().getEventDate().toString(),
                invitation.getInvitedBy().getFirstName() + " " +
                        invitation.getInvitedBy().getLastName(),
                invitation.getInvitedUser().getEmail(),
                false,
                null,
                language);

        eventProducer.sendInvitationCreated(event);
    }

}