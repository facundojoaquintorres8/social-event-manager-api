package com.socialeventmanager.auth.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.socialeventmanager.auth.dto.AuthResponseDTO;
import com.socialeventmanager.auth.dto.LoginRequestDTO;
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
    private final AuthenticationManager authenticationManager;

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
                buildAuthResponse(user));
    }

    @Override
    public ApiResponseDTO<AuthResponseDTO> login(LoginRequestDTO request) {
        String email = request.getEmail().trim().toLowerCase();

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        email,
                        request.getPassword()));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found"));

        return new ApiResponseDTO<>(
                true,
                "Login successful",
                buildAuthResponse(user));
    }

    private AuthResponseDTO buildAuthResponse(User user) {
        return AuthResponseDTO.builder()
                .token(jwtService.generateToken(user.getEmail()))
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .build();
    }

}