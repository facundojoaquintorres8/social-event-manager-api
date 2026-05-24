package com.socialeventmanager.event.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.socialeventmanager.event.dto.ExternalInvitationPreviewResponseDTO;
import com.socialeventmanager.event.service.ExternalInvitationService;
import com.socialeventmanager.shared.dto.ApiResponseDTO;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/external-invitations")
@RequiredArgsConstructor
public class ExternalInvitationController {

    private final ExternalInvitationService externalInvitationService;

    @GetMapping("/{token}")
    public ResponseEntity<ApiResponseDTO<ExternalInvitationPreviewResponseDTO>> getInvitationPreview(
            @PathVariable String token) {
        return ResponseEntity.ok(externalInvitationService.getInvitationPreview(token));
    }

}