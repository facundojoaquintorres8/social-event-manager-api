package com.socialeventmanager.event.dto;

import com.socialeventmanager.event.enums.InvitationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateInvitationStatusRequestDTO {

    @NotNull(message = "Status is required")
    private InvitationStatus status;
}