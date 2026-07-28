package com.socialeventmanager.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponseDTO {

    private String accessToken;
    private String refreshToken;
    private String email;
    private String firstName;
    private String lastName;
    private boolean hasPassword;
}