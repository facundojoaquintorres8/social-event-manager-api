package com.socialeventmanager.event.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RemoveInvitationRequestDTO {

    @NotNull(message = "Event ID is required")
    private UUID eventId;

    @NotBlank(message = "Email is required")
    private String email;
}