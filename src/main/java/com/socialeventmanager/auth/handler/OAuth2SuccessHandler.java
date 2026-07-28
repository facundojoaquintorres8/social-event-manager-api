package com.socialeventmanager.auth.handler;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.socialeventmanager.auth.dto.AuthResponseDTO;
import com.socialeventmanager.auth.enums.Provider;
import com.socialeventmanager.auth.service.AuthService;
import com.socialeventmanager.shared.dto.ApiResponseDTO;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        OAuth2User oAuth2User = token.getPrincipal();

        String registrationId = token.getAuthorizedClientRegistrationId();
        Provider provider = Provider.valueOf(registrationId.toUpperCase());

        String providerId;
        String email;
        String firstName;
        String lastName;

        if (provider == Provider.GITHUB) {
            providerId = String.valueOf(oAuth2User.getAttribute("id"));
            email = oAuth2User.getAttribute("email");
            String name = oAuth2User.getAttribute("name");
            if (name != null && name.contains(" ")) {
                firstName = name.substring(0, name.indexOf(" "));
                lastName = name.substring(name.indexOf(" ") + 1);
            } else {
                firstName = name != null ? name : oAuth2User.getAttribute("login");
                lastName = "";
            }
        } else {
            providerId = oAuth2User.getAttribute("sub");
            email = oAuth2User.getAttribute("email");
            firstName = oAuth2User.getAttribute("given_name");
            lastName = oAuth2User.getAttribute("family_name");
        }

        if (email == null) {
            response.sendRedirect(frontendUrl + "/login?error=oauthEmailRequired");
            return;
        }

        ApiResponseDTO<AuthResponseDTO> authResponse = authService.processOAuth2Login(
                provider, providerId, email, firstName, lastName);

        AuthResponseDTO data = authResponse.getData();

        String redirectUrl = UriComponentsBuilder
                .fromUriString(frontendUrl + "/oauth2/callback")
                .queryParam("accessToken", data.getAccessToken())
                .queryParam("refreshToken", data.getRefreshToken())
                .queryParam("firstName", data.getFirstName())
                .queryParam("lastName", data.getLastName())
                .queryParam("email", data.getEmail())
                .queryParam("hasPassword", data.isHasPassword())
                .build().toUriString();

        response.sendRedirect(redirectUrl);
    }
}