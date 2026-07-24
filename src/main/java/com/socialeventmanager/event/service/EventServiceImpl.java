package com.socialeventmanager.event.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.socialeventmanager.auth.service.CurrentUserService;
import com.socialeventmanager.event.dto.BalanceParticipantDTO;
import com.socialeventmanager.event.dto.BalanceParticipantRequestDTO;
import com.socialeventmanager.event.dto.BalanceRequestDTO;
import com.socialeventmanager.event.dto.BalanceResponseDTO;
import com.socialeventmanager.event.dto.CalendarEventResponseDTO;
import com.socialeventmanager.event.dto.ContributionResponseDTO;
import com.socialeventmanager.event.dto.CreateEventRequestDTO;
import com.socialeventmanager.event.dto.DashboardResponseDTO;
import com.socialeventmanager.event.dto.EventDetailsFullResponseDTO;
import com.socialeventmanager.event.dto.EventParticipantResponseDTO;
import com.socialeventmanager.event.dto.EventResponseDTO;
import com.socialeventmanager.event.dto.InvitationResponseDTO;
import com.socialeventmanager.event.dto.InviteUserRequestDTO;
import com.socialeventmanager.event.dto.SettlementDTO;
import com.socialeventmanager.event.entity.Event;
import com.socialeventmanager.event.entity.EventInvitation;
import com.socialeventmanager.event.enums.EventStatus;
import com.socialeventmanager.event.repository.EventRepository;
import com.socialeventmanager.event.repository.EventSpecification;
import com.socialeventmanager.kafka.event.EventCancelledEvent;
import com.socialeventmanager.kafka.producer.EventProducer;
import com.socialeventmanager.shared.dto.ApiResponseDTO;
import com.socialeventmanager.shared.exception.BadRequestException;
import com.socialeventmanager.shared.exception.ForbiddenException;
import com.socialeventmanager.shared.util.Constants;
import com.socialeventmanager.shared.util.EmailValidator;
import com.socialeventmanager.shared.util.EventValidator;
import com.socialeventmanager.user.entity.User;
import com.socialeventmanager.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final ExternalInvitationService externalInvitationService;
    private final InvitationService invitationService;
    private final ContributionService contributionService;
    private final EventValidator eventValidator;
    private final EventProducer eventProducer;

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
                .language(request.getLanguage())
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

        size = Math.min(size, Constants.DEFAULT_PAGE_SIZE);

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
    public ApiResponseDTO<Page<EventResponseDTO>> getAttendingEvents(
            int page,
            int size,
            String sortBy,
            String direction) {

        Page<EventResponseDTO> events = invitationService.findAllAccepted(page,
                size,
                sortBy,
                direction)
                .map(invitation -> mapToResponse(invitation.getEvent()));

        return new ApiResponseDTO<>(
                true,
                "Attending events retrieved successfully",
                events);
    }

    @Override
    public ApiResponseDTO<List<CalendarEventResponseDTO>> getCalendarEvents(
            LocalDateTime from,
            LocalDateTime to) {

        User currentUser = getCurrentUser();

        if (from == null || to == null) {
            LocalDate now = LocalDate.now();

            from = now.withDayOfMonth(1).atStartOfDay();

            to = now.withDayOfMonth(now.lengthOfMonth())
                    .atTime(LocalTime.MAX);
        }

        List<Event> ownedEvents = eventRepository
                .findAllByCreatedByAndStatusNotAndEventDateBetween(
                        currentUser,
                        EventStatus.CANCELLED,
                        from,
                        to);

        List<EventInvitation> invitedEvents = invitationService.findAllToCalendarEvents(
                currentUser,
                from,
                to);

        List<CalendarEventResponseDTO> response = Stream.concat(
                ownedEvents.stream()
                        .map(event -> CalendarEventResponseDTO.builder()
                                .id(event.getId())
                                .title(event.getTitle())
                                .eventDate(event.getEventDate())
                                .location(event.getLocation())
                                .eventStatus(event.getStatus())
                                .owner(true)
                                .build()),
                invitedEvents.stream()
                        .map(invitation -> CalendarEventResponseDTO.builder()
                                .id(invitation.getEvent().getId())
                                .title(invitation.getEvent().getTitle())
                                .eventDate(invitation.getEvent().getEventDate())
                                .location(invitation.getEvent().getLocation())
                                .eventStatus(invitation.getEvent().getStatus())
                                .invitationStatus(invitation.getStatus())
                                .owner(false)
                                .build()))
                .sorted(Comparator.comparing(CalendarEventResponseDTO::getEventDate))
                .toList();

        return new ApiResponseDTO<>(
                true,
                "Calendar events retrieved successfully",
                response);
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

        List<EventParticipantResponseDTO> participants = new ArrayList<>();
        participants.addAll(invitationService.findAllByEventAndNotCancelled(event));
        participants.addAll(externalInvitationService.findAllByEventAndPending(event));

        List<ContributionResponseDTO> contributions = contributionService.findAllByEvent(event);

        return new ApiResponseDTO<>(
                true,
                "Event retrieved successfully",
                mapToFullResponse(event, participants, contributions));
    }

    @Override
    @Transactional
    public ApiResponseDTO<EventResponseDTO> updateEvent(
            UUID eventId,
            CreateEventRequestDTO request) {
        Event event = getOwnedEvent(eventId);

        eventValidator.validateEventAllowsInteraction(event);

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
    @Transactional
    public ApiResponseDTO<Void> deleteEvent(UUID eventId, String language) {
        Event event = getOwnedEvent(eventId);

        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new BadRequestException("Event already cancelled");
        }

        event.setStatus(EventStatus.CANCELLED);

        eventRepository.save(event);

        List<String> participantEmails = invitationService.getAcceptedParticipantEmails(event);

        invitationService.cancelInvitationsForEvent(event);
        externalInvitationService.cancelExternalInvitationsForEvent(event);

        if (!participantEmails.isEmpty()) {
            eventProducer.sendEventCancelled(new EventCancelledEvent(
                    event.getId(),
                    event.getTitle(),
                    event.getLocation(),
                    event.getLatitude(),
                    event.getLongitude(),
                    event.getEventDate().toString(),
                    event.getCreatedBy().getFirstName() + " " +
                            event.getCreatedBy().getLastName(),
                    participantEmails,
                    language));
        }

        return new ApiResponseDTO<>(
                true,
                "Event cancelled successfully",
                null);
    }

    @Override
    public ApiResponseDTO<Void> inviteUser(
            UUID eventId,
            InviteUserRequestDTO request, String language) {
        User currentUser = getCurrentUser();

        Event event = eventRepository
                .findByIdAndCreatedBy(eventId, currentUser)
                .orElseThrow(() -> new BadRequestException("Event not found"));

        eventValidator.validateEventAllowsInteraction(event);

        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        EmailValidator.validateEmail(email);

        Optional<User> invitedUserOptional = userRepository.findByEmail(email);

        if (invitedUserOptional.isPresent()) {
            User invitedUser = invitedUserOptional.get();
            if (invitedUser.getId().equals(currentUser.getId())) {
                throw new BadRequestException("You cannot invite yourself");
            }
            return invitationService.inviteExistingUser(event, currentUser, invitedUser, language);
        }

        return externalInvitationService.inviteExternalUser(event, currentUser, email, language);
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

        List<InvitationResponseDTO> recentInvitations = invitationService.getRecentInvitations(currentUser);

        DashboardResponseDTO dashboard = new DashboardResponseDTO(
                totalEvents,
                activeEvents,
                cancelledEvents,
                upcomingEvents,
                recentEvents,
                recentInvitations);

        return new ApiResponseDTO<>(
                true,
                "Dashboard data retrieved successfully",
                dashboard);

    }

    @Override
    public ApiResponseDTO<BalanceResponseDTO> calculateBalance(
            UUID eventId,
            BalanceRequestDTO request) {
        if (request.getParticipants().isEmpty()) {
            throw new BadRequestException("At least one participant is required");
        }

        Event event = getAccessibleEvent(eventId);

        List<ContributionResponseDTO> splitContributions = contributionService.findAllByEvent(event)
                .stream()
                .filter(ContributionResponseDTO::isSplitCost)
                .filter(c -> c.getCost() != null)
                .toList();

        Set<UUID> participantIds = request.getParticipants()
                .stream()
                .map(BalanceParticipantRequestDTO::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<String> missingParticipants = splitContributions.stream()
                .filter(contribution -> !participantIds.contains(contribution.getCreatedById()))
                .map(ContributionResponseDTO::getCreatedBy)
                .distinct()
                .toList();
        if (!missingParticipants.isEmpty()) {
            throw new BadRequestException(
                    "Cannot exclude participants who have shared expenses: "
                            + String.join(", ", missingParticipants));
        }

        BigDecimal totalCost = splitContributions.stream()
                .map(ContributionResponseDTO::getCost)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int participantCount = request.getParticipants().size();

        BigDecimal costPerPerson = totalCost.divide(
                BigDecimal.valueOf(participantCount),
                2,
                RoundingMode.HALF_UP);

        Map<UUID, BigDecimal> paidByUser = new HashMap<>();
        for (ContributionResponseDTO contribution : splitContributions) {
            paidByUser.merge(
                    contribution.getCreatedById(),
                    contribution.getCost(),
                    BigDecimal::add);
        }

        List<BalanceParticipantDTO> balances = request.getParticipants()
                .stream()
                .map(participant -> {

                    BigDecimal paid = participant.getUserId() != null
                            ? paidByUser.getOrDefault(
                                    participant.getUserId(),
                                    BigDecimal.ZERO)
                            : BigDecimal.ZERO;

                    BigDecimal balance = paid.subtract(costPerPerson);

                    return BalanceParticipantDTO.builder()
                            .name(participant.getName())
                            .paid(paid)
                            .shouldPay(costPerPerson)
                            .balance(balance)
                            .build();
                })
                .toList();

        List<SettlementDTO> settlements = new ArrayList<>();

        List<BalanceParticipantDTO> creditors = balances.stream()
                .filter(balance -> balance.getBalance().compareTo(BigDecimal.ZERO) > 0)
                .toList();
        List<BalanceParticipantDTO> debtors = balances.stream()
                .filter(balance -> balance.getBalance().compareTo(BigDecimal.ZERO) < 0)
                .toList();

        List<BalanceParticipantDTO> mutableCreditors = creditors.stream()
                .map(balance -> BalanceParticipantDTO.builder()
                        .name(balance.getName())
                        .paid(balance.getPaid())
                        .shouldPay(balance.getShouldPay())
                        .balance(balance.getBalance())
                        .build())
                .toList();
        List<BalanceParticipantDTO> mutableDebtors = debtors.stream()
                .map(balance -> BalanceParticipantDTO.builder()
                        .name(balance.getName())
                        .paid(balance.getPaid())
                        .shouldPay(balance.getShouldPay())
                        .balance(balance.getBalance())
                        .build())
                .toList();

        int creditorIndex = 0;
        int debtorIndex = 0;

        while (creditorIndex < mutableCreditors.size()
                && debtorIndex < mutableDebtors.size()) {

            BalanceParticipantDTO creditor = mutableCreditors.get(creditorIndex);

            BalanceParticipantDTO debtor = mutableDebtors.get(debtorIndex);

            BigDecimal creditorAmount = creditor.getBalance();

            BigDecimal debtorAmount = debtor.getBalance().abs();

            BigDecimal transferAmount = creditorAmount.min(debtorAmount);

            settlements.add(
                    SettlementDTO.builder()
                            .from(debtor.getName())
                            .to(creditor.getName())
                            .amount(transferAmount)
                            .build());

            creditor.setBalance(
                    creditorAmount.subtract(transferAmount));

            debtor.setBalance(
                    debtor.getBalance().add(transferAmount));

            if (creditor.getBalance().compareTo(BigDecimal.ZERO) == 0) {
                creditorIndex++;
            }

            if (debtor.getBalance().compareTo(BigDecimal.ZERO) == 0) {
                debtorIndex++;
            }
        }

        return new ApiResponseDTO<>(
                true,
                "Balance calculated successfully",
                BalanceResponseDTO.builder()
                        .totalCost(totalCost)
                        .participantCount(participantCount)
                        .costPerPerson(costPerPerson)
                        .balances(balances)
                        .settlements(settlements)
                        .build());
    }

    private User getCurrentUser() {
        return currentUserService.getCurrentUser();
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

    private EventDetailsFullResponseDTO mapToFullResponse(Event event, List<EventParticipantResponseDTO> participants,
            List<ContributionResponseDTO> contributions) {
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
                .createdById(event.getCreatedBy().getId())
                .createdBy(event.getCreatedBy().getFirstName() + " "
                        + event.getCreatedBy().getLastName())
                .status(event.getStatus())
                .participants(participants)
                .contributions(contributions)
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

        boolean invited = invitationService.existsByEventIdAndInvitedUserAndNotCancelled(
                eventId,
                currentUser);

        if (invited) {
            return eventRepository.findById(eventId)
                    .orElseThrow(() -> new BadRequestException("Event not found"));
        }

        throw new ForbiddenException("You do not have access to this event");
    }

}