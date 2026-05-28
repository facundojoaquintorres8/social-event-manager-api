package com.socialeventmanager.event.service;

import java.util.List;
import java.util.UUID;

import com.socialeventmanager.event.dto.ContributionResponseDTO;
import com.socialeventmanager.event.dto.CreateContributionRequestDTO;
import com.socialeventmanager.event.entity.Event;
import com.socialeventmanager.shared.dto.ApiResponseDTO;

public interface ContributionService {

    ApiResponseDTO<Void> createContribution(
            UUID eventId,
            CreateContributionRequestDTO request);

    ApiResponseDTO<Void> updateContribution(
            UUID eventId,
            UUID contributionId,
            CreateContributionRequestDTO request);

    ApiResponseDTO<Void> deleteContribution(
            UUID eventId,
            UUID contributionId);

    List<ContributionResponseDTO> findAllByEvent(Event event);
}