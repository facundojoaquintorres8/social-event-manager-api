package com.socialeventmanager.user.dto;

import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponseDTO {
    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
}