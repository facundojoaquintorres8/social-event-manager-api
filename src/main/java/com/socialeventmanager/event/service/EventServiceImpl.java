package com.socialeventmanager.event.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.socialeventmanager.event.dto.CreateEventRequestDTO;
import com.socialeventmanager.event.dto.EventParticipantResponseDTO;
import com.socialeventmanager.event.dto.EventResponseDTO;
import com.socialeventmanager.event.dto.InvitationResponseDTO;
import com.socialeventmanager.event.dto.InviteUserRequestDTO;
import com.socialeventmanager.event.dto.UpdateInvitationStatusRequestDTO;
import com.socialeventmanager.event.entity.Event;
import com.socialeventmanager.event.entity.EventInvitation;
import com.socialeventmanager.event.enums.InvitationStatus;
import com.socialeventmanager.event.repository.EventInvitationRepository;
import com.socialeventmanager.event.repository.EventRepository;
import com.socialeventmanager.event.repository.EventSpecification;
import com.socialeventmanager.shared.dto.ApiResponseDTO;
import com.socialeventmanager.shared.exception.BadRequestException;
import com.socialeventmanager.user.entity.User;
import com.socialeventmanager.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final EventInvitationRepository invitationRepository;

    @Override
    public ApiResponseDTO<EventResponseDTO> createEvent(
            CreateEventRequestDTO request) {
        User currentUser = getCurrentUser();

        Event event = Event.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .eventDate(request.getEventDate())
                .location(request.getLocation())
                .createdBy(currentUser)
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
                    LocalDateTime toDate) {
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
            Event event = getOwnedEvent(eventId);

            return new ApiResponseDTO<>(
                            true,
                            "Event retrieved successfully",
                            mapToResponse(event));
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

            eventRepository.save(event);

            return new ApiResponseDTO<>(
                            true,
                            "Event updated successfully",
                            mapToResponse(event));
    }

    @Override
    public ApiResponseDTO<Void> deleteEvent(UUID eventId) {
            Event event = getOwnedEvent(eventId);

            eventRepository.delete(event);

            return new ApiResponseDTO<>(
                            true,
                            "Event deleted successfully",
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

            String email = request.getEmail().trim().toLowerCase();

            User invitedUser = userRepository.findByEmail(email)
                            .orElseThrow(() -> new BadRequestException("User not found"));

            if (invitedUser.getId().equals(currentUser.getId())) {
                    throw new BadRequestException("You cannot invite yourself");
            }

            boolean alreadyInvited = invitationRepository
                            .findByEventAndInvitedUser(event, invitedUser)
                            .isPresent();

            if (alreadyInvited) {
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

    @Override
    public ApiResponseDTO<Page<InvitationResponseDTO>> getMyInvitations(
            int page,
            int size,
            String sortBy,
            String direction
    ) {
        User currentUser = getCurrentUser();
    
        size = Math.min(size, 50);
    
        List<String> allowedSortFields = List.of(
                "status",
                "createdAt"
        );
    
        if (!allowedSortFields.contains(sortBy)) {
            throw new BadRequestException("Invalid sort field");
        }
    
        Sort sort = direction.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
    
        Pageable pageable = PageRequest.of(page, size, sort);
    
        Page<InvitationResponseDTO> invitations = invitationRepository
                .findAllByInvitedUser(currentUser, pageable)
                .map(invitation -> InvitationResponseDTO.builder()
                        .invitationId(invitation.getId())
                        .eventId(invitation.getEvent().getId())
                        .title(invitation.getEvent().getTitle())
                        .eventDate(invitation.getEvent().getEventDate())
                        .location(invitation.getEvent().getLocation())
                        .invitedBy(invitation.getInvitedBy().getEmail())
                        .status(invitation.getStatus())
                        .build()
                );
    
        return new ApiResponseDTO<>(
                true,
                "Invitations retrieved successfully",
                invitations
        );
    }

    @Override
    public ApiResponseDTO<Void> updateInvitationStatus(
                    UUID invitationId,
                    UpdateInvitationStatusRequestDTO request) {
            User currentUser = getCurrentUser();

            EventInvitation invitation = invitationRepository
                            .findByIdAndInvitedUser(invitationId, currentUser)
                            .orElseThrow(() -> new BadRequestException("Invitation not found"));

            if (request.getStatus() == InvitationStatus.PENDING) {
                    throw new BadRequestException("Invalid invitation status");
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
    public ApiResponseDTO<Page<EventParticipantResponseDTO>> getEventParticipants(
            UUID eventId,
            int page,
            int size,
            String sortBy,
            String direction
    ) {
        Event event = getOwnedEvent(eventId);
    
        size = Math.min(size, 50);
    
        List<String> allowedSortFields = List.of(
                "status",
                "createdAt"
        );
    
        if (!allowedSortFields.contains(sortBy)) {
            throw new BadRequestException("Invalid sort field");
        }
    
        Sort sort = direction.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
    
        Pageable pageable = PageRequest.of(page, size, sort);
    
        Page<EventParticipantResponseDTO> participants = invitationRepository
                .findAllByEvent(event, pageable)
                .map(invitation -> EventParticipantResponseDTO.builder()
                        .firstName(invitation.getInvitedUser().getFirstName())
                        .lastName(invitation.getInvitedUser().getLastName())
                        .email(invitation.getInvitedUser().getEmail())
                        .status(invitation.getStatus())
                        .build());
    
        return new ApiResponseDTO<>(
                true,
                "Participants retrieved successfully",
                participants
        );
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
                .createdBy(event.getCreatedBy().getEmail())
                .build();
    }

    private Event getOwnedEvent(UUID eventId) {
        User currentUser = getCurrentUser();
    
        return eventRepository
                .findByIdAndCreatedBy(eventId, currentUser)
                .orElseThrow(() ->
                        new BadRequestException("Event not found"));
    }
}