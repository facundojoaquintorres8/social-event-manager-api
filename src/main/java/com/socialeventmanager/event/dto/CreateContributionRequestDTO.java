package com.socialeventmanager.event.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateContributionRequestDTO {

    @NotBlank(message = "Contribution name is required")
    @Size(max = 100)
    private String name;

    @Size(max = 255)
    private String description;

    @DecimalMin(value = "0.0", inclusive = true, message = "Cost must be greater than or equal to 0")
    private BigDecimal cost;

    private Boolean splitCost = false;
}