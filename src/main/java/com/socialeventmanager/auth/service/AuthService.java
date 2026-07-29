package com.socialeventmanager.auth.service;

import com.socialeventmanager.auth.dto.AuthResponseDTO;
import com.socialeventmanager.auth.dto.ForgotPasswordRequestDTO;
import com.socialeventmanager.auth.dto.LoginRequestDTO;
import com.socialeventmanager.auth.dto.RefreshRequestDTO;
import com.socialeventmanager.auth.dto.RegisterRequestDTO;
import com.socialeventmanager.auth.dto.ResetPasswordRequestDTO;
import com.socialeventmanager.auth.enums.Provider;
import com.socialeventmanager.shared.dto.ApiResponseDTO;

public interface AuthService {

    ApiResponseDTO<AuthResponseDTO> register(RegisterRequestDTO request, String language);

    ApiResponseDTO<AuthResponseDTO> login(LoginRequestDTO request);

    ApiResponseDTO<AuthResponseDTO> refreshToken(RefreshRequestDTO request);

    ApiResponseDTO<Void> forgotPassword(ForgotPasswordRequestDTO request, String language);

    ApiResponseDTO<Void> resetPassword(ResetPasswordRequestDTO request);

    ApiResponseDTO<AuthResponseDTO> processOAuth2Login(
            Provider provider,
            String providerId,
            String email,
            String firstName,
            String lastName,
            String language);
}