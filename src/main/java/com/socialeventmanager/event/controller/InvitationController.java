package com.socialeventmanager.event.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.socialeventmanager.event.dto.InvitationResponseDTO;
import com.socialeventmanager.event.dto.RemoveInvitationRequestDTO;
import com.socialeventmanager.event.dto.UpdateInvitationStatusRequestDTO;
import com.socialeventmanager.event.enums.InvitationStatus;
import com.socialeventmanager.event.service.InvitationService;
import com.socialeventmanager.shared.dto.ApiResponseDTO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/invitations")
@RequiredArgsConstructor
public class InvitationController {

    private final InvitationService invitationService;

    @GetMapping("/invitations")
    public ResponseEntity<ApiResponseDTO<Page<InvitationResponseDTO>>> getMyInvitations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) InvitationStatus status) {
        return ResponseEntity.ok(
                invitationService.getMyInvitations(
                        page,
                        size,
                        sortBy,
                        direction,
                        status));
    }

    @PutMapping("/{eventId}/invitations")
    public ResponseEntity<ApiResponseDTO<Void>> updateInvitationStatus(
            @PathVariable UUID eventId,
            @Valid @RequestBody UpdateInvitationStatusRequestDTO request) {
        return ResponseEntity.ok(
                invitationService.updateInvitationStatus(
                        eventId,
                        request));
    }

    @DeleteMapping("/{eventId}/invite")
    public ResponseEntity<ApiResponseDTO<Void>> removeInvitation(
            @PathVariable UUID eventId,
            @Valid @RequestBody RemoveInvitationRequestDTO request) {
        return ResponseEntity.ok(invitationService.removeInvitation(eventId, request));
    }

}