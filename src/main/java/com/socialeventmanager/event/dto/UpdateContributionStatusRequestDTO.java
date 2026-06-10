package com.socialeventmanager.event.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateContributionStatusRequestDTO {
    @NotNull(message = "Completed is required")
    private Boolean completed;
}