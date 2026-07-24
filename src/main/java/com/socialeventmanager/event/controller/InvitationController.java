package com.socialeventmanager.event.controller;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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

    @GetMapping("/me")
    public ResponseEntity<ApiResponseDTO<Page<InvitationResponseDTO>>> getMyInvitations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
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

    @PutMapping()
    public ResponseEntity<ApiResponseDTO<Void>> updateInvitationStatus(
            @Valid @RequestBody UpdateInvitationStatusRequestDTO request,
            @RequestHeader(value = "Accept-Language", defaultValue = "en") String language) {
        return ResponseEntity.ok(invitationService.updateInvitationStatus(request, language));
    }

    @DeleteMapping()
    public ResponseEntity<ApiResponseDTO<Void>> removeInvitation(
            @Valid @RequestBody RemoveInvitationRequestDTO request) {
        return ResponseEntity.ok(invitationService.removeInvitation(request));
    }

}