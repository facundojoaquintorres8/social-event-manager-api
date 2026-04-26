package com.socialeventmanager.auth.service;

import com.socialeventmanager.auth.dto.AuthResponseDTO;
import com.socialeventmanager.auth.dto.RegisterRequestDTO;
import com.socialeventmanager.shared.dto.ApiResponseDTO;

public interface AuthService {

    ApiResponseDTO<AuthResponseDTO> register(RegisterRequestDTO request);
}