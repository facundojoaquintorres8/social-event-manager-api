package com.socialeventmanager.auth.service;

import com.socialeventmanager.auth.dto.RegisterRequestDTO;
import com.socialeventmanager.shared.dto.ApiResponseDTO;

public interface AuthService {

    ApiResponseDTO<String> register(RegisterRequestDTO request);
}