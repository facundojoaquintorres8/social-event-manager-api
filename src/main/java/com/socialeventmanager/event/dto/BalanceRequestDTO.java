package com.socialeventmanager.event.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class BalanceRequestDTO {

    @NotEmpty(message = "atLeastOneParticipant")
    private List<BalanceParticipantRequestDTO> participants;
}