package com.socialeventmanager.auth.service;

import com.socialeventmanager.auth.dto.AuthResponseDTO;
import com.socialeventmanager.auth.dto.LoginRequestDTO;
import com.socialeventmanager.auth.dto.RefreshRequestDTO;
import com.socialeventmanager.auth.dto.RegisterRequestDTO;
import com.socialeventmanager.event.service.ExternalInvitationService;
import com.socialeventmanager.shared.dto.ApiResponseDTO;
import com.socialeventmanager.shared.exception.BadRequestException;
import com.socialeventmanager.token.entity.Token;
import com.socialeventmanager.token.enums.TokenType;
import com.socialeventmanager.token.repository.TokenRepository;
import com.socialeventmanager.user.entity.User;
import com.socialeventmanager.user.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final TokenRepository tokenRepository;
    private final ExternalInvitationService externalInvitationService;

    @Override
    @Transactional
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

        externalInvitationService.claimExternalInvitations(user);

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

    @Override
    public ApiResponseDTO<AuthResponseDTO> refreshToken(RefreshRequestDTO request) {

        String refreshToken = request.getRefreshToken();
        String userEmail = jwtService.extractUsername(refreshToken);

        if (userEmail == null) {
            throw new BadRequestException("Invalid refresh token");
        }

        if (!"REFRESH".equals(jwtService.extractTokenType(refreshToken))) {
            throw new BadRequestException("Invalid refresh token");
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BadRequestException("User not found"));

        if (!jwtService.isTokenValid(refreshToken, user)) {
            throw new BadRequestException("Invalid refresh token");
        }

        String newAccessToken = jwtService.generateToken(user.getEmail());

        revokeAllUserTokens(user);
        saveUserToken(user, newAccessToken);

        AuthResponseDTO response = AuthResponseDTO.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .build();

        return new ApiResponseDTO<>(
                true,
                "Token refreshed successfully",
                response);
    }

    private AuthResponseDTO buildAuthResponse(User user) {
        String accessToken = jwtService.generateToken(user.getEmail());
        String refreshToken = jwtService.generateRefreshToken(user.getEmail());

        revokeAllUserTokens(user);
        saveUserToken(user, accessToken);

        return AuthResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .build();
    }

    private void saveUserToken(User user, String jwtToken) {
        Token token = Token.builder()
                .user(user)
                .tokenValue(jwtToken)
                .tokenType(TokenType.BEARER)
                .expired(false)
                .revoked(false)
                .build();

        tokenRepository.save(token);
    }

    private void revokeAllUserTokens(User user) {
        List<Token> validUserTokens = tokenRepository.findAllByUserIdAndExpiredFalseAndRevokedFalse(user.getId());

        if (validUserTokens.isEmpty()) {
            return;
        }

        validUserTokens.forEach(token -> {
            token.setExpired(true);
            token.setRevoked(true);
        });

        tokenRepository.saveAll(validUserTokens);
    }

}