package com.socialeventmanager.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponseDTO {

    private String token;
    private String email;
    private String firstName;
    private String lastName;
}