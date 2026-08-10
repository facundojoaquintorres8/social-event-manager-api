package com.socialeventmanager.event.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.socialeventmanager.auth.service.CurrentUserService;
import com.socialeventmanager.event.dto.ContributionResponseDTO;
import com.socialeventmanager.event.dto.CreateContributionRequestDTO;
import com.socialeventmanager.event.dto.UpdateContributionStatusRequestDTO;
import com.socialeventmanager.event.entity.Contribution;
import com.socialeventmanager.event.entity.Event;
import com.socialeventmanager.event.entity.EventInvitation;
import com.socialeventmanager.event.enums.InvitationStatus;
import com.socialeventmanager.event.repository.ContributionRepository;
import com.socialeventmanager.event.repository.EventRepository;
import com.socialeventmanager.event.repository.InvitationRepository;
import com.socialeventmanager.kafka.event.NotificationEvent;
import com.socialeventmanager.kafka.producer.EventProducer;
import com.socialeventmanager.notification.enums.NotificationType;
import com.socialeventmanager.shared.dto.ApiResponseDTO;
import com.socialeventmanager.shared.exception.BadRequestException;
import com.socialeventmanager.shared.util.EventValidator;
import com.socialeventmanager.user.entity.User;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class ContributionServiceImpl implements ContributionService {

    private static final String CONTRIBUTION_NAME = "contributionName";
    private static final String CONTRIBUTION_NOT_FOUND = "contributionNotFound";
    private static final String EVENT_TITLE = "eventTitle";
    private static final String PARTICIPANT_NAME = "participantName";
    private static final String EVENT_NOT_FOUND = "eventNotFound";

    private final ContributionRepository contributionRepository;
    private final EventRepository eventRepository;
    private final InvitationRepository invitationRepository;
    private final CurrentUserService currentUserService;
    private final EventValidator eventValidator;
    private final EventProducer eventProducer;
    private final InvitationService invitationService;

    @Override
    public ApiResponseDTO<Void> createContribution(
            UUID eventId,
            CreateContributionRequestDTO request) {

        User currentUser = currentUserService.getCurrentUser();

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BadRequestException(EVENT_NOT_FOUND));

        eventValidator.validateEventAllowsContributions(event);

        validateUserCanManageContributions(event, currentUser);

        Contribution contribution = Contribution.builder()
                .event(event)
                .createdBy(currentUser)
                .name(request.getName().trim())
                .description(request.getDescription())
                .cost(request.getCost())
                .splitCost(Boolean.TRUE.equals(request.getSplitCost())
                        && request.getCost() != null)
                .completed(false)
                .build();

        contributionRepository.save(contribution);

        List<UUID> recipientIds = getEventParticipantIds(event);
        eventProducer.sendNotification(new NotificationEvent(
                event.getId(),
                NotificationType.CONTRIBUTION_ADDED,
                Map.of(
                        EVENT_TITLE, event.getTitle(),
                        CONTRIBUTION_NAME, contribution.getName(),
                        PARTICIPANT_NAME, currentUser.getFirstName() + " " + currentUser.getLastName()),
                recipientIds));

        return new ApiResponseDTO<>(
                true,
                "Contribution created successfully",
                null);
    }

    @Override
    public ApiResponseDTO<Void> updateContribution(
            UUID eventId,
            UUID contributionId,
            CreateContributionRequestDTO request) {

        User currentUser = currentUserService.getCurrentUser();

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BadRequestException(EVENT_NOT_FOUND));

        eventValidator.validateEventAllowsContributions(event);

        Contribution contribution = contributionRepository.findById(contributionId)
                .orElseThrow(() -> new BadRequestException(CONTRIBUTION_NOT_FOUND));

        if (!contribution.getEvent().getId().equals(event.getId())) {
            throw new BadRequestException("contributionNotInEvent");
        }

        validateUserCanEditContribution(
                event,
                contribution,
                currentUser);

        contribution.setName(request.getName().trim());

        contribution.setDescription(request.getDescription());

        contribution.setCost(request.getCost());

        contribution.setSplitCost(
                Boolean.TRUE.equals(request.getSplitCost())
                        && request.getCost() != null);

        contributionRepository.save(contribution);

        List<UUID> recipientIds = getEventParticipantIds(contribution.getEvent());
        eventProducer.sendNotification(new NotificationEvent(
                contribution.getEvent().getId(),
                NotificationType.CONTRIBUTION_EDITED,
                Map.of(
                        EVENT_TITLE, contribution.getEvent().getTitle(),
                        CONTRIBUTION_NAME, contribution.getName(),
                        PARTICIPANT_NAME, currentUser.getFirstName() + " " + currentUser.getLastName()),
                recipientIds));

        return new ApiResponseDTO<>(
                true,
                "Contribution updated successfully",
                null);
    }

    @Override
    public ApiResponseDTO<Void> updateContributionStatus(
            UUID eventId,
            UUID contributionId,
            UpdateContributionStatusRequestDTO request) {

        User currentUser = currentUserService.getCurrentUser();

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BadRequestException(EVENT_NOT_FOUND));

        eventValidator.validateEventAllowsContributions(event);

        Contribution contribution = contributionRepository.findById(contributionId)
                .orElseThrow(() -> new BadRequestException(CONTRIBUTION_NOT_FOUND));

        if (!contribution.getEvent().getId().equals(event.getId())) {
            throw new BadRequestException("contributionNotInEvent");
        }

        validateUserCanEditContribution(
                event,
                contribution,
                currentUser);

        contribution.setCompleted(request.getCompleted());
        contributionRepository.save(contribution);

        if (Boolean.TRUE.equals(request.getCompleted())) {
            List<UUID> recipientIds = getEventParticipantIds(contribution.getEvent());
            eventProducer.sendNotification(new NotificationEvent(
                    contribution.getEvent().getId(),
                    NotificationType.CONTRIBUTION_COMPLETED,
                    Map.of(
                            EVENT_TITLE, contribution.getEvent().getTitle(),
                            CONTRIBUTION_NAME, contribution.getName()),
                    recipientIds));
        }

        return new ApiResponseDTO<>(
                true,
                "Contribution status updated",
                null);
    }

    @Override
    public ApiResponseDTO<Void> deleteContribution(
            UUID eventId,
            UUID contributionId) {

        User currentUser = currentUserService.getCurrentUser();

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BadRequestException(EVENT_NOT_FOUND));

        eventValidator.validateEventAllowsContributions(event);

        Contribution contribution = contributionRepository.findById(contributionId)
                .orElseThrow(() -> new BadRequestException(CONTRIBUTION_NOT_FOUND));

        if (!contribution.getEvent().getId().equals(event.getId())) {
            throw new BadRequestException("contributionNotInEvent");
        }

        validateUserCanEditContribution(
                event,
                contribution,
                currentUser);

        List<UUID> recipientIds = getEventParticipantIds(contribution.getEvent());
        eventProducer.sendNotification(new NotificationEvent(
                contribution.getEvent().getId(),
                NotificationType.CONTRIBUTION_DELETED,
                Map.of(
                        EVENT_TITLE, contribution.getEvent().getTitle(),
                        CONTRIBUTION_NAME, contribution.getName(),
                        PARTICIPANT_NAME, currentUser.getFirstName() + " " + currentUser.getLastName()),
                recipientIds));

        contributionRepository.delete(contribution);

        return new ApiResponseDTO<>(
                true,
                "Contribution deleted successfully",
                null);
    }

    @Override
    public List<ContributionResponseDTO> findAllByEvent(Event event) {

        UUID currentUserId = currentUserService.getCurrentUser().getId();

        return contributionRepository
                .findAllByEventOrderByCompletedAscCreatedAtAsc(event)
                .stream()
                .map(contribution -> ContributionResponseDTO.builder()
                        .id(contribution.getId())
                        .name(contribution.getName())
                        .description(contribution.getDescription())
                        .cost(contribution.getCost())
                        .splitCost(contribution.isSplitCost())
                        .completed(contribution.isCompleted())
                        .createdById(contribution.getCreatedBy().getId())
                        .createdBy(
                                contribution.getCreatedBy().getFirstName()
                                        + " "
                                        + contribution.getCreatedBy().getLastName())
                        .createdByEmail(
                                contribution.getCreatedBy().getEmail())
                        .owner(
                                contribution.getCreatedBy().getId()
                                        .equals(currentUserId))
                        .build())
                .toList();
    }

    private void validateUserCanManageContributions(
            Event event,
            User user) {

        if (event.getCreatedBy().getId().equals(user.getId())) {
            return;
        }

        EventInvitation invitation = invitationRepository
                .findByEventAndInvitedUser(event, user)
                .orElseThrow(() -> new BadRequestException("notPartOfEvent"));

        if (invitation.getStatus() != InvitationStatus.ACCEPTED) {
            throw new BadRequestException("onlyAcceptedCanManage");
        }
    }

    private void validateUserCanEditContribution(
            Event event,
            Contribution contribution,
            User user) {

        if (event.getCreatedBy().getId().equals(user.getId())) {
            return;
        }

        if (!contribution.getCreatedBy().getId().equals(user.getId())) {
            throw new BadRequestException("cannotEditContribution");
        }

        EventInvitation invitation = invitationRepository
                .findByEventAndInvitedUser(event, user)
                .orElseThrow(() -> new BadRequestException("notPartOfEvent"));

        if (invitation.getStatus() != InvitationStatus.ACCEPTED) {
            throw new BadRequestException("onlyAcceptedCanEdit");
        }
    }

    private List<UUID> getEventParticipantIds(Event event) {
        List<UUID> ids = new ArrayList<>(invitationService.getAcceptedParticipantIds(event));
        UUID organizerId = event.getCreatedBy().getId();
        if (!ids.contains(organizerId)) {
            ids.add(organizerId);
        }
        return ids;
    }
}
