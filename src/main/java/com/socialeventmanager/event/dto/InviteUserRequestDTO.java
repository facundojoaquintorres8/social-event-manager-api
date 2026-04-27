package com.socialeventmanager.event.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InviteUserRequestDTO {

    @Email(message = "Invalid email") // TODO
    @NotBlank(message = "Email is required")
    private String email;
}