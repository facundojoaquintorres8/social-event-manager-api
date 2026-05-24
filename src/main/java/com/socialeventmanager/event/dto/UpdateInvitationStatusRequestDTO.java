package com.socialeventmanager.event.dto;

import java.util.UUID;

import com.socialeventmanager.event.enums.InvitationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateInvitationStatusRequestDTO {

    @NotNull(message = "Event ID is required")
    private UUID eventId;

    @NotNull(message = "Status is required")
    private InvitationStatus status;
}