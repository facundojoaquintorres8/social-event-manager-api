package com.socialeventmanager.auth.dto;

import com.socialeventmanager.auth.enums.Provider;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OAuth2LoginRequestDTO {
    private Provider provider;
    private String providerId;
    private String email;
    private String firstName;
    private String lastName;
    private String language;
    private String ip;
    private String userAgent;
}