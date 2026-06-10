package com.socialeventmanager.event.service;

import java.util.List;
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
import com.socialeventmanager.shared.dto.ApiResponseDTO;
import com.socialeventmanager.shared.exception.BadRequestException;
import com.socialeventmanager.shared.util.EventValidator;
import com.socialeventmanager.user.entity.User;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class ContributionServiceImpl implements ContributionService {

    private final ContributionRepository contributionRepository;
    private final EventRepository eventRepository;
    private final InvitationRepository invitationRepository;

    private final CurrentUserService currentUserService;
    private final EventValidator eventValidator;

    @Override
    public ApiResponseDTO<Void> createContribution(
            UUID eventId,
            CreateContributionRequestDTO request) {

        User currentUser = currentUserService.getCurrentUser();

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BadRequestException("Event not found"));

        eventValidator.validateEventAllowsInteraction(event);

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
                .orElseThrow(() -> new BadRequestException("Event not found"));

        eventValidator.validateEventAllowsInteraction(event);

        Contribution contribution = contributionRepository.findById(contributionId)
                .orElseThrow(() -> new BadRequestException("Contribution not found"));

        if (!contribution.getEvent().getId().equals(event.getId())) {
            throw new BadRequestException("Contribution does not belong to this event");
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
                .orElseThrow(() -> new BadRequestException("Event not found"));

        eventValidator.validateEventAllowsInteraction(event);

        Contribution contribution = contributionRepository.findById(contributionId)
                .orElseThrow(() -> new BadRequestException("Contribution not found"));

        if (!contribution.getEvent().getId().equals(event.getId())) {
            throw new BadRequestException("Contribution does not belong to this event");
        }

        validateUserCanEditContribution(
                event,
                contribution,
                currentUser);

        contribution.setCompleted(request.getCompleted());
        contributionRepository.save(contribution);

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
                .orElseThrow(() -> new BadRequestException("Event not found"));

        eventValidator.validateEventAllowsInteraction(event);

        Contribution contribution = contributionRepository.findById(contributionId)
                .orElseThrow(() -> new BadRequestException("Contribution not found"));

        if (!contribution.getEvent().getId().equals(event.getId())) {
            throw new BadRequestException(
                    "Contribution does not belong to this event");
        }

        validateUserCanEditContribution(
                event,
                contribution,
                currentUser);

        contributionRepository.delete(contribution);

        return new ApiResponseDTO<>(
                true,
                "Contribution deleted successfully",
                null);
    }

    @Override
    public List<ContributionResponseDTO> findAllByEvent(Event event) {

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
                        .createdBy(
                                contribution.getCreatedBy().getFirstName()
                                        + " "
                                        + contribution.getCreatedBy().getLastName())
                        .createdByEmail(
                                contribution.getCreatedBy().getEmail())
                        .owner(
                                contribution.getCreatedBy().getId()
                                        .equals(currentUserService.getCurrentUser().getId()))
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
                .orElseThrow(() -> new BadRequestException("You are not part of this event"));

        if (invitation.getStatus() != InvitationStatus.ACCEPTED) {
            throw new BadRequestException(
                    "Only accepted participants can manage contributions");
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
            throw new BadRequestException(
                    "You cannot edit this contribution");
        }

        EventInvitation invitation = invitationRepository
                .findByEventAndInvitedUser(event, user)
                .orElseThrow(() -> new BadRequestException("You are not part of this event"));

        if (invitation.getStatus() != InvitationStatus.ACCEPTED) {
            throw new BadRequestException(
                    "Only accepted participants can edit contributions");
        }
    }
}
