package com.socialeventmanager.user.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class UserResponseDTO {
    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private boolean hasPassword;
    private boolean premium;
    private LocalDateTime premiumSince;
}