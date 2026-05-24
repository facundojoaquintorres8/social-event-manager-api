package com.socialeventmanager.event.service;

import com.socialeventmanager.event.dto.*;
import com.socialeventmanager.event.entity.Event;
import com.socialeventmanager.event.entity.EventInvitation;
import com.socialeventmanager.event.entity.ExternalInvitation;
import com.socialeventmanager.event.enums.EventStatus;
import com.socialeventmanager.event.enums.ExternalInvitationStatus;
import com.socialeventmanager.event.enums.InvitationStatus;
import com.socialeventmanager.event.repository.EventInvitationRepository;
import com.socialeventmanager.event.repository.EventInvitationSpecification;
import com.socialeventmanager.event.repository.EventRepository;
import com.socialeventmanager.event.repository.EventSpecification;
import com.socialeventmanager.event.repository.ExternalInvitationRepository;
import com.socialeventmanager.shared.dto.ApiResponseDTO;
import com.socialeventmanager.shared.exception.BadRequestException;
import com.socialeventmanager.shared.exception.ForbiddenException;
import com.socialeventmanager.user.entity.User;
import com.socialeventmanager.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final EventInvitationRepository invitationRepository;
    private final ExternalInvitationRepository externalInvitationRepository;

    @Override
    public ApiResponseDTO<EventResponseDTO> createEvent(
            CreateEventRequestDTO request) {
        User currentUser = getCurrentUser();

        Event event = Event.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .eventDate(request.getEventDate())
                .location(request.getLocation())
                .locationAddress(request.getLocationAddress())
                .placeId(request.getPlaceId())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .createdBy(currentUser)
                .status(EventStatus.ACTIVE)
                .build();

        eventRepository.save(event);

        return new ApiResponseDTO<>(
                true,
                "Event created successfully",
                mapToResponse(event));
    }

    @Override
    public ApiResponseDTO<Page<EventResponseDTO>> getMyEvents(
            int page,
            int size,
            String sortBy,
            String direction,
            String title,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            EventStatus status) {
        User currentUser = getCurrentUser();

        size = Math.min(size, 50);

        List<String> allowedSortFields = List.of(
                "title",
                "eventDate",
                "createdAt",
                "location");

        if (!allowedSortFields.contains(sortBy)) {
            throw new BadRequestException("Invalid sort field");
        }

        Sort sort = direction.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<Event> spec = Specification.unrestricted();

        spec = spec.and(EventSpecification.hasUser(currentUser));

        if (status != null) {
            spec = spec.and(EventSpecification.hasStatus(status));
        }

        if (title != null && !title.isBlank()) {
            spec = spec.and(EventSpecification.titleContains(title));
        }

        if (fromDate != null) {
            spec = spec.and(EventSpecification.dateAfter(fromDate));
        }

        if (toDate != null) {
            spec = spec.and(EventSpecification.dateBefore(toDate));
        }

        Page<EventResponseDTO> events = eventRepository
                .findAll(spec, pageable)
                .map(this::mapToResponse);

        return new ApiResponseDTO<>(
                true,
                "Events retrieved successfully",
                events);
    }

    @Override
    public ApiResponseDTO<EventResponseDTO> getEventById(UUID eventId) {
        Event event = getAccessibleEvent(eventId);

        return new ApiResponseDTO<>(
                true,
                "Event retrieved successfully",
                mapToResponse(event));
    }

    @Override
    public ApiResponseDTO<EventDetailsFullResponseDTO> getEventByIdFull(UUID eventId) {
        Event event = getAccessibleEvent(eventId);

        List<EventParticipantResponseDTO> participants = invitationRepository
                .findAllByEventAndStatusNot(event, InvitationStatus.CANCELLED)
                .stream()
                .map(invitation -> EventParticipantResponseDTO
                        .builder()
                        .firstName(invitation.getInvitedUser().getFirstName())
                        .lastName(invitation.getInvitedUser().getLastName())
                        .email(invitation.getInvitedUser().getEmail())
                        .status(invitation.getStatus())
                        .build())
                .toList();

        return new ApiResponseDTO<>(
                true,
                "Event retrieved successfully",
                mapToFullResponse(event, participants));
    }

    @Override
    public ApiResponseDTO<EventResponseDTO> updateEvent(
            UUID eventId,
            CreateEventRequestDTO request) {
        Event event = getOwnedEvent(eventId);

        event.setTitle(request.getTitle());
        event.setDescription(request.getDescription());
        event.setEventDate(request.getEventDate());
        event.setLocation(request.getLocation());
        event.setLocationAddress(request.getLocationAddress());
        event.setPlaceId(request.getPlaceId());
        event.setLatitude(request.getLatitude());
        event.setLongitude(request.getLongitude());

        eventRepository.save(event);

        return new ApiResponseDTO<>(
                true,
                "Event updated successfully",
                mapToResponse(event));
    }

    @Override
    public ApiResponseDTO<Void> deleteEvent(UUID eventId) {
        Event event = getOwnedEvent(eventId);

        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new BadRequestException("Event already cancelled");
        }

        event.setStatus(EventStatus.CANCELLED);

        eventRepository.save(event);

        List<EventInvitation> invitations = invitationRepository.findAllByEvent(event);

        for (EventInvitation invitation : invitations) {
            invitation.setStatus(InvitationStatus.CANCELLED);
        }

        invitationRepository.saveAll(invitations);

        return new ApiResponseDTO<>(
                true,
                "Event cancelled successfully",
                null);
    }

    @Override
    public ApiResponseDTO<Void> inviteUser(
            UUID eventId,
            InviteUserRequestDTO request) {
        User currentUser = getCurrentUser();

        Event event = eventRepository
                .findByIdAndCreatedBy(eventId, currentUser)
                .orElseThrow(() -> new BadRequestException("Event not found"));

        String email = request.getEmail()
                .trim()
                .toLowerCase();

        Optional<User> invitedUserOptional = userRepository.findByEmail(email);

        if (invitedUserOptional.isPresent()
                && invitedUserOptional.get()
                        .getId()
                        .equals(currentUser.getId())) {
            throw new BadRequestException(
                    "You cannot invite yourself");
        }

        if (invitedUserOptional.isPresent()) {
            return inviteExistingUser(
                    event,
                    currentUser,
                    invitedUserOptional.get());
        }

        return inviteExternalUser(
                event,
                currentUser,
                email);
    }

    @Override
    public ApiResponseDTO<Page<InvitationResponseDTO>> getMyInvitations(
            int page,
            int size,
            String sortBy,
            String direction,
            InvitationStatus status) {
        User currentUser = getCurrentUser();

        size = Math.min(size, 50);

        List<String> allowedSortFields = List.of(
                "status",
                "createdAt");

        if (!allowedSortFields.contains(sortBy)) {
            throw new BadRequestException("Invalid sort field");
        }

        Sort sort = direction.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<EventInvitation> spec = Specification.<EventInvitation>unrestricted()
                .and(EventInvitationSpecification.hasUser(currentUser));

        if (status != null) {
            spec = spec.and(EventInvitationSpecification.hasStatus(status));
        } else {
            spec = spec.and(EventInvitationSpecification.hasNotStatus(InvitationStatus.CANCELLED));
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
                        .invitedBy(invitation.getInvitedBy().getEmail())
                        .status(invitation.getStatus())
                        .build());

        return new ApiResponseDTO<>(
                true,
                "Invitations retrieved successfully",
                invitations);
    }

    @Override
    public ApiResponseDTO<Void> updateInvitationStatus(
            UUID invitationId,
            UpdateInvitationStatusRequestDTO request) {
        User currentUser = getCurrentUser();

        if (request.getStatus() == InvitationStatus.PENDING) {
            throw new BadRequestException("Invalid invitation status");
        }

        EventInvitation invitation = invitationRepository
                .findByIdAndInvitedUser(invitationId, currentUser)
                .orElseThrow(() -> new BadRequestException("Invitation not found"));

        if (invitation.getStatus() == InvitationStatus.CANCELLED) {
            throw new BadRequestException("Invitation is cancelled");
        }

        if (invitation.getStatus() == request.getStatus()) {
            throw new BadRequestException("Invitation already has this status");
        }

        invitation.setStatus(request.getStatus());

        invitationRepository.save(invitation);

        return new ApiResponseDTO<>(
                true,
                "Invitation updated successfully",
                null);
    }

    @Override
    public ApiResponseDTO<Page<EventResponseDTO>> getAttendingEvents(
            int page,
            int size,
            String sortBy,
            String direction) {
        User currentUser = getCurrentUser();

        size = Math.min(size, 50);

        List<String> allowedSortFields = List.of(
                "eventDate",
                "title",
                "location");

        if (!allowedSortFields.contains(sortBy)) {
            throw new BadRequestException("Invalid sort field");
        }

        Sort sort = direction.equalsIgnoreCase("asc")
                ? Sort.by("event." + sortBy).ascending()
                : Sort.by("event." + sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<EventResponseDTO> events = invitationRepository
                .findAllByInvitedUserAndStatus(
                        currentUser,
                        InvitationStatus.ACCEPTED,
                        pageable)
                .map(invitation -> mapToResponse(invitation.getEvent()));

        return new ApiResponseDTO<>(
                true,
                "Attending events retrieved successfully",
                events);
    }

    @Override
    public ApiResponseDTO<Void> removeInvitation(
            UUID eventId,
            RemoveInvitationRequestDTO request) {
        User currentUser = getCurrentUser();

        Event event = eventRepository
                .findByIdAndCreatedBy(eventId, currentUser)
                .orElseThrow(() -> new BadRequestException("Event not found"));

        String email = request.getEmail()
                .trim()
                .toLowerCase();

        Optional<User> invitedUserOptional = userRepository.findByEmail(email);

        if (invitedUserOptional.isPresent()) {
            return removeExistingInvitation(
                    event,
                    invitedUserOptional.get());
        }

        return removeExternalInvitation(
                event,
                email);
    }

    @Override
    public ApiResponseDTO<DashboardResponseDTO> getDashboard() {

        User currentUser = getCurrentUser();

        long totalEvents = eventRepository.countByCreatedById(currentUser.getId());

        long activeEvents = eventRepository.countByCreatedByIdAndStatus(
                currentUser.getId(),
                EventStatus.ACTIVE);

        long cancelledEvents = eventRepository.countByCreatedByIdAndStatus(
                currentUser.getId(),
                EventStatus.CANCELLED);

        long upcomingEvents = eventRepository.countByCreatedByIdAndEventDateAfter(
                currentUser.getId(),
                LocalDateTime.now());

        List<EventResponseDTO> recentEvents = eventRepository
                .findTop5ByCreatedByIdOrderByCreatedAtDesc(currentUser.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();

        DashboardResponseDTO dashboard = new DashboardResponseDTO(
                totalEvents,
                activeEvents,
                cancelledEvents,
                upcomingEvents,
                recentEvents);

        return new ApiResponseDTO<>(
                true,
                "Dashboard data retrieved successfully",
                dashboard);

    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found"));
    }

    private EventResponseDTO mapToResponse(Event event) {
        return EventResponseDTO.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .eventDate(event.getEventDate())
                .location(event.getLocation())
                .locationAddress(event.getLocationAddress())
                .placeId(event.getPlaceId())
                .latitude(event.getLatitude())
                .longitude(event.getLongitude())
                .createdBy(event.getCreatedBy().getFirstName() + " "
                        + event.getCreatedBy().getLastName())
                .status(event.getStatus())
                .build();
    }

    private EventDetailsFullResponseDTO mapToFullResponse(Event event, List<EventParticipantResponseDTO> participants) {
        return EventDetailsFullResponseDTO.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .eventDate(event.getEventDate())
                .location(event.getLocation())
                .locationAddress(event.getLocationAddress())
                .placeId(event.getPlaceId())
                .latitude(event.getLatitude())
                .longitude(event.getLongitude())
                .createdBy(event.getCreatedBy().getFirstName() + " "
                        + event.getCreatedBy().getLastName())
                .status(event.getStatus())
                .participants(participants)
                .owner(event.getCreatedBy().getId().equals(getCurrentUser().getId()))
                .build();
    }

    private Event getOwnedEvent(UUID eventId) {
        User currentUser = getCurrentUser();

        return eventRepository
                .findByIdAndCreatedBy(eventId, currentUser)
                .orElseThrow(() -> new BadRequestException("Event not found"));
    }

    private Event getAccessibleEvent(UUID eventId) {
        User currentUser = getCurrentUser();

        Optional<Event> ownedEvent = eventRepository.findByIdAndCreatedBy(
                eventId,
                currentUser);

        if (ownedEvent.isPresent()) {
            return ownedEvent.get();
        }

        boolean invited = invitationRepository.existsByEventIdAndInvitedUserAndStatusNot(
                eventId,
                currentUser,
                InvitationStatus.CANCELLED);

        if (invited) {
            return eventRepository.findById(eventId)
                    .orElseThrow(() -> new BadRequestException("Event not found"));
        }

        throw new ForbiddenException("You do not have access to this event");
    }

    private ApiResponseDTO<Void> inviteExistingUser(
            Event event,
            User currentUser,
            User invitedUser) {
        EventInvitation existingInvitation = invitationRepository
                .findByEventAndInvitedUser(event, invitedUser)
                .orElse(null);

        if (existingInvitation != null) {

            if (existingInvitation.getStatus() == InvitationStatus.CANCELLED
                    || existingInvitation.getStatus() == InvitationStatus.REJECTED) {
                existingInvitation.setStatus(InvitationStatus.PENDING);

                invitationRepository.save(existingInvitation);

                return new ApiResponseDTO<>(
                        true,
                        "User invited successfully",
                        null);
            }

            throw new BadRequestException("User already invited");
        }

        EventInvitation invitation = EventInvitation.builder()
                .event(event)
                .invitedUser(invitedUser)
                .invitedBy(currentUser)
                .status(InvitationStatus.PENDING)
                .build();

        invitationRepository.save(invitation);

        return new ApiResponseDTO<>(
                true,
                "User invited successfully",
                null);
    }

    private ApiResponseDTO<Void> inviteExternalUser(
            Event event,
            User currentUser,
            String email) {
        ExternalInvitation existingInvitation = externalInvitationRepository
                .findByEventAndInvitedEmail(event, email)
                .orElse(null);

        if (existingInvitation != null) {

            if (existingInvitation.getStatus() == ExternalInvitationStatus.CANCELLED
                    || existingInvitation.getStatus() == ExternalInvitationStatus.EXPIRED) {

                existingInvitation.setStatus(ExternalInvitationStatus.PENDING);

                existingInvitation.setExpiresAt(event.getEventDate().plusDays(1));

                externalInvitationRepository.save(existingInvitation);

                return new ApiResponseDTO<>(
                        true,
                        "Invitation sent successfully",
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
                .expiresAt(event.getEventDate().plusDays(1))
                .build();

        externalInvitationRepository.save(invitation);

        return new ApiResponseDTO<>(
                true,
                "Invitation sent successfully",
                null);
    }

    private ApiResponseDTO<Void> removeExistingInvitation(
            Event event,
            User invitedUser) {
        EventInvitation invitation = invitationRepository
                .findByEventAndInvitedUser(
                        event,
                        invitedUser)
                .orElseThrow(() -> new BadRequestException(
                        "Invitation not found"));

        if (invitation.getStatus() == InvitationStatus.CANCELLED) {
            throw new BadRequestException(
                    "Invitation already cancelled");
        }

        invitation.setStatus(
                InvitationStatus.CANCELLED);

        invitationRepository.save(invitation);

        return new ApiResponseDTO<>(
                true,
                "Invitation cancelled successfully",
                null);
    }

    private ApiResponseDTO<Void> removeExternalInvitation(
            Event event,
            String email) {
        ExternalInvitation invitation = externalInvitationRepository
                .findByEventAndInvitedEmail(
                        event,
                        email)
                .orElseThrow(() -> new BadRequestException(
                        "Invitation not found"));

        if (invitation.getStatus() == ExternalInvitationStatus.CANCELLED) {
            throw new BadRequestException(
                    "Invitation already cancelled");
        }

        invitation.setStatus(
                ExternalInvitationStatus.CANCELLED);

        externalInvitationRepository.save(invitation);

        return new ApiResponseDTO<>(
                true,
                "Invitation cancelled successfully",
                null);
    }
}