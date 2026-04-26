package com.socialeventmanager.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.socialeventmanager.auth.dto.AuthResponseDTO;
import com.socialeventmanager.auth.dto.RegisterRequestDTO;
import com.socialeventmanager.shared.dto.ApiResponseDTO;
import com.socialeventmanager.shared.exception.BadRequestException;
import com.socialeventmanager.user.entity.User;
import com.socialeventmanager.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    public ApiResponseDTO<AuthResponseDTO> register(RegisterRequestDTO request) {

        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email already registered");
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        userRepository.save(user);

        return new ApiResponseDTO<>(
                true,
                "User registered successfully",
                AuthResponseDTO.builder()
                        .email(email)
                        .firstName(request.getFirstName())
                        .lastName(request.getLastName())
                        .token(jwtService.generateToken(email))
                        .build());
    }
}