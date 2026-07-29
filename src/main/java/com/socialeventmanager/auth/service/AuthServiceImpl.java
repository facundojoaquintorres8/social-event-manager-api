package com.socialeventmanager.auth.service;

import com.socialeventmanager.auth.dto.AuthResponseDTO;
import com.socialeventmanager.auth.dto.ForgotPasswordRequestDTO;
import com.socialeventmanager.auth.dto.LoginRequestDTO;
import com.socialeventmanager.auth.dto.RefreshRequestDTO;
import com.socialeventmanager.auth.dto.RegisterRequestDTO;
import com.socialeventmanager.auth.dto.ResetPasswordRequestDTO;
import com.socialeventmanager.auth.entity.PasswordResetToken;
import com.socialeventmanager.auth.entity.UserProvider;
import com.socialeventmanager.auth.enums.Provider;
import com.socialeventmanager.auth.repository.PasswordResetTokenRepository;
import com.socialeventmanager.auth.repository.UserProviderRepository;
import com.socialeventmanager.event.service.ExternalInvitationService;
import com.socialeventmanager.kafka.event.PasswordResetRequestedEvent;
import com.socialeventmanager.kafka.event.UserRegisteredEvent;
import com.socialeventmanager.kafka.producer.EventProducer;
import com.socialeventmanager.shared.dto.ApiResponseDTO;
import com.socialeventmanager.shared.exception.BadRequestException;
import com.socialeventmanager.shared.util.EmailValidator;
import com.socialeventmanager.token.entity.Token;
import com.socialeventmanager.token.enums.TokenType;
import com.socialeventmanager.token.repository.TokenRepository;
import com.socialeventmanager.user.entity.User;
import com.socialeventmanager.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final TokenRepository tokenRepository;
    private final ExternalInvitationService externalInvitationService;
    private final EventProducer eventProducer;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UserProviderRepository userProviderRepository;

    @Override
    @Transactional
    public ApiResponseDTO<AuthResponseDTO> register(RegisterRequestDTO request, String language) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        EmailValidator.validateEmail(request.getEmail());

        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("emailAlreadyRegistered");
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        userRepository.save(user);

        externalInvitationService.claimExternalInvitations(user, language);

        eventProducer.sendUserRegistered(new UserRegisteredEvent(
                user.getId(),
                user.getFirstName(),
                user.getEmail(),
                language));

        return new ApiResponseDTO<>(
                true,
                "User registered successfully",
                buildAuthResponse(user));
    }

    @Override
    public ApiResponseDTO<AuthResponseDTO> login(LoginRequestDTO request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        email,
                        request.getPassword()));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("userNotFound"));

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
            throw new BadRequestException("invalidRefreshToken");
        }

        if (!"REFRESH".equals(jwtService.extractTokenType(refreshToken))) {
            throw new BadRequestException("invalidRefreshToken");
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BadRequestException("userNotFound"));

        if (!jwtService.isTokenValid(refreshToken, user)) {
            throw new BadRequestException("invalidRefreshToken");
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

    @Override
    public ApiResponseDTO<Void> forgotPassword(ForgotPasswordRequestDTO request, String language) {
        String email = request.getEmail().trim().toLowerCase();

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return new ApiResponseDTO<>(true, "Password reset email sent if account exists", null);
        }

        User user = userOpt.get();

        passwordResetTokenRepository.deleteByUserId(user.getId());

        String token = UUID.randomUUID().toString();

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();

        passwordResetTokenRepository.save(resetToken);

        eventProducer.sendPasswordResetRequested(new PasswordResetRequestedEvent(
                user.getEmail(),
                user.getFirstName(),
                token,
                language));

        return new ApiResponseDTO<>(true, "Password reset email sent if account exists", null);
    }

    @Override
    @Transactional
    public ApiResponseDTO<Void> resetPassword(ResetPasswordRequestDTO request) {
        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByToken(request.getToken())
                .orElseThrow(() -> new BadRequestException("invalidOrExpiredToken"));

        if (resetToken.isUsed()) {
            throw new BadRequestException("invalidOrExpiredToken");
        }

        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("invalidOrExpiredToken");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        tokenRepository.findAllByUserId(user.getId())
                .forEach(token -> {
                    token.setRevoked(true);
                    token.setExpired(true);
                });

        return new ApiResponseDTO<>(true, "Password reset successfully", null);
    }

    @Override
    @Transactional
    public ApiResponseDTO<AuthResponseDTO> processOAuth2Login(
            Provider provider,
            String providerId,
            String email,
            String firstName,
            String lastName,
            String language) {

        Optional<UserProvider> existingProvider = userProviderRepository
                .findByProviderAndProviderId(provider, providerId);

        User user;

        if (existingProvider.isPresent()) {
            user = existingProvider.get().getUser();
        } else {
            Optional<User> existingUser = userRepository.findByEmail(email);

            if (existingUser.isPresent()) {
                user = existingUser.get();
            } else {
                user = User.builder()
                        .firstName(firstName != null ? firstName : "User")
                        .lastName(lastName != null ? lastName : "")
                        .email(email)
                        .password(null)
                        .hasPassword(false)
                        .build();
                userRepository.save(user);
            }

            UserProvider userProvider = UserProvider.builder()
                    .user(user)
                    .provider(provider)
                    .providerId(providerId)
                    .build();
            userProviderRepository.save(userProvider);

            externalInvitationService.claimExternalInvitations(user, language);
        }

        return new ApiResponseDTO<>(true, "Login successful", buildAuthResponse(user));
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
                .hasPassword(user.isHasPassword())
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