package com.socialeventmanager.event.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.socialeventmanager.event.dto.CreateContributionRequestDTO;
import com.socialeventmanager.event.service.ContributionService;
import com.socialeventmanager.shared.dto.ApiResponseDTO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/events/{eventId}/contributions")
@RequiredArgsConstructor
public class ContributionController {

    private final ContributionService contributionService;

    @PostMapping
    public ResponseEntity<ApiResponseDTO<Void>> createContribution(
            @PathVariable UUID eventId,
            @Valid @RequestBody CreateContributionRequestDTO request) {

        return ResponseEntity.ok(
                contributionService.createContribution(eventId, request));
    }

    @PutMapping("/{contributionId}")
    public ResponseEntity<ApiResponseDTO<Void>> updateContribution(
            @PathVariable UUID eventId,
            @PathVariable UUID contributionId,
            @Valid @RequestBody CreateContributionRequestDTO request) {

        return ResponseEntity.ok(
                contributionService.updateContribution(
                        eventId,
                        contributionId,
                        request));
    }

    @DeleteMapping("/{contributionId}")
    public ResponseEntity<ApiResponseDTO<Void>> deleteContribution(
            @PathVariable UUID eventId,
            @PathVariable UUID contributionId) {

        return ResponseEntity.ok(
                contributionService.deleteContribution(
                        eventId,
                        contributionId));
    }
}