package com.socialeventmanager.event.dto;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ContributionResponseDTO {
    private UUID id;
    private String name;
    private String description;
    private BigDecimal cost;
    private boolean splitCost;
    private boolean completed;
    private String createdBy;
    private String createdByEmail;
    private boolean owner;
}