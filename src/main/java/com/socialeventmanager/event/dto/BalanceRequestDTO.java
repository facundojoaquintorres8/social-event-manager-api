package com.socialeventmanager.event.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class BalanceRequestDTO {

    @NotEmpty(message = "At least one participant is required")
    private List<BalanceParticipantRequestDTO> participants;
}