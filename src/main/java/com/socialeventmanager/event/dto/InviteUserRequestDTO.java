package com.socialeventmanager.event.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InviteUserRequestDTO {

    @NotBlank(message = "Email is required")
    private String email;
}