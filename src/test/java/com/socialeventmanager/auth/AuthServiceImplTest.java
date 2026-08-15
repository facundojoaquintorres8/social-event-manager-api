package com.socialeventmanager.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.socialeventmanager.auth.dto.AuthResponseDTO;
import com.socialeventmanager.auth.dto.ForgotPasswordRequestDTO;
import com.socialeventmanager.auth.dto.LoginRequestDTO;
import com.socialeventmanager.auth.dto.OAuth2LoginRequestDTO;
import com.socialeventmanager.auth.dto.RegisterRequestDTO;
import com.socialeventmanager.auth.dto.ResetPasswordRequestDTO;
import com.socialeventmanager.auth.entity.PasswordResetToken;
import com.socialeventmanager.auth.entity.UserProvider;
import com.socialeventmanager.auth.enums.Provider;
import com.socialeventmanager.auth.repository.PasswordResetTokenRepository;
import com.socialeventmanager.auth.repository.UserProviderRepository;
import com.socialeventmanager.auth.service.AuthServiceImpl;
import com.socialeventmanager.auth.service.JwtService;
import com.socialeventmanager.event.service.ExternalInvitationService;
import com.socialeventmanager.kafka.producer.EventProducer;
import com.socialeventmanager.shared.dto.ApiResponseDTO;
import com.socialeventmanager.shared.exception.BadRequestException;
import com.socialeventmanager.shared.util.Constants;
import com.socialeventmanager.token.repository.TokenRepository;
import com.socialeventmanager.user.entity.User;
import com.socialeventmanager.user.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl")
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private TokenRepository tokenRepository;
    @Mock
    private ExternalInvitationService externalInvitationService;
    @Mock
    private EventProducer eventProducer;
    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock
    private UserProviderRepository userProviderRepository;
    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private AuthServiceImpl authService;

    private User user;
    private RegisterRequestDTO registerRequest;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .firstName("Facundo")
                .lastName("Torres")
                .email("facundo@test.com")
                .password("encodedPassword")
                .hasPassword(true)
                .build();
        user.setId(UUID.randomUUID());

        registerRequest = new RegisterRequestDTO();
        registerRequest.setFirstName("Facundo");
        registerRequest.setLastName("Torres");
        registerRequest.setEmail("facundo@test.com");
        registerRequest.setPassword("Password1");
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("should register user successfully")
        void shouldRegisterUserSuccessfully() {
            when(userRepository.existsByEmail("facundo@test.com")).thenReturn(false);
            when(passwordEncoder.encode("Password1")).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenReturn(user);
            when(tokenRepository.findAllByUserIdAndExpiredFalseAndRevokedFalse(any())).thenReturn(List.of());
            when(jwtService.generateToken(any())).thenReturn("accessToken");
            when(jwtService.generateRefreshToken(any())).thenReturn("refreshToken");
            when(tokenRepository.save(any())).thenReturn(null);

            ApiResponseDTO<AuthResponseDTO> response = authService.register(registerRequest, "en");

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData().getEmail()).isEqualTo("facundo@test.com");
            verify(userRepository).save(any(User.class));
            verify(eventProducer).sendUserRegistered(any());
            verify(externalInvitationService).claimExternalInvitations(any(), eq("en"));
        }

        @Test
        @DisplayName("should throw when email already registered")
        void shouldThrowWhenEmailAlreadyRegistered() {
            when(userRepository.existsByEmail("facundo@test.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(registerRequest, "en"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("emailAlreadyRegistered");

            verify(userRepository, never()).save(any());
            verify(eventProducer, never()).sendUserRegistered(any());
        }

        @Test
        @DisplayName("should normalize email to lowercase")
        void shouldNormalizeEmailToLowercase() {
            registerRequest.setEmail("FACUNDO@TEST.COM");
            when(userRepository.existsByEmail("facundo@test.com")).thenReturn(false);
            when(passwordEncoder.encode(any())).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenReturn(user);
            when(tokenRepository.findAllByUserIdAndExpiredFalseAndRevokedFalse(any())).thenReturn(List.of());
            when(jwtService.generateToken(any())).thenReturn("accessToken");
            when(jwtService.generateRefreshToken(any())).thenReturn("refreshToken");
            when(tokenRepository.save(any())).thenReturn(null);

            authService.register(registerRequest, "en");

            verify(userRepository).existsByEmail("facundo@test.com");
        }
    }

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("should login successfully")
        void shouldLoginSuccessfully() {
            LoginRequestDTO request = new LoginRequestDTO();
            request.setEmail("facundo@test.com");
            request.setPassword("Password1");

            when(userRepository.findByEmail("facundo@test.com")).thenReturn(Optional.of(user));
            when(tokenRepository.findAllByUserIdAndExpiredFalseAndRevokedFalse(any())).thenReturn(List.of());
            when(jwtService.generateToken(any())).thenReturn("accessToken");
            when(jwtService.generateRefreshToken(any())).thenReturn("refreshToken");
            when(tokenRepository.save(any())).thenReturn(null);

            ApiResponseDTO<AuthResponseDTO> response = authService.login(request);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData().getEmail()).isEqualTo("facundo@test.com");
        }

        @Test
        @DisplayName("should throw when credentials are invalid")
        void shouldThrowWhenCredentialsAreInvalid() {
            LoginRequestDTO request = new LoginRequestDTO();
            request.setEmail("facundo@test.com");
            request.setPassword("wrongPassword");

            doThrow(new BadCredentialsException("Bad credentials"))
                    .when(authenticationManager)
                    .authenticate(any(UsernamePasswordAuthenticationToken.class));

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BadCredentialsException.class);
        }
    }

    @Nested
    @DisplayName("forgotPassword")
    class ForgotPassword {

        @Test
        @DisplayName("should return success even when email not found")
        void shouldReturnSuccessEvenWhenEmailNotFound() {
            ForgotPasswordRequestDTO request = new ForgotPasswordRequestDTO();
            request.setEmail("notfound@test.com");

            when(userRepository.findByEmail("notfound@test.com")).thenReturn(Optional.empty());

            ApiResponseDTO<Void> response = authService.forgotPassword(request, "en");

            assertThat(response.isSuccess()).isTrue();
            verify(eventProducer, never()).sendPasswordResetRequested(any());
        }

        @Test
        @DisplayName("should send reset email when user exists")
        void shouldSendResetEmailWhenUserExists() {
            ForgotPasswordRequestDTO request = new ForgotPasswordRequestDTO();
            request.setEmail("facundo@test.com");

            when(userRepository.findByEmail("facundo@test.com")).thenReturn(Optional.of(user));
            when(passwordResetTokenRepository.save(any())).thenReturn(null);

            ApiResponseDTO<Void> response = authService.forgotPassword(request, "en");

            assertThat(response.isSuccess()).isTrue();
            verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
            verify(eventProducer).sendPasswordResetRequested(any());
        }
    }

    @Nested
    @DisplayName("resetPassword")
    class ResetPassword {

        @Test
        @DisplayName("should reset password successfully")
        void shouldResetPasswordSuccessfully() {
            ResetPasswordRequestDTO request = new ResetPasswordRequestDTO();
            request.setToken("valid-token");
            request.setNewPassword("NewPassword123");

            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .token("valid-token")
                    .user(user)
                    .expiresAt(LocalDateTime.now(Constants.TIMEZONE_ARGENTINA).plusHours(1))
                    .used(false)
                    .build();

            when(passwordResetTokenRepository.findByToken("valid-token"))
                    .thenReturn(Optional.of(resetToken));
            when(passwordEncoder.encode("NewPassword123")).thenReturn("newEncodedPassword");
            when(userRepository.save(any())).thenReturn(user);
            when(tokenRepository.findAllByUserId(any())).thenReturn(List.of());

            ApiResponseDTO<Void> response = authService.resetPassword(request);

            assertThat(response.isSuccess()).isTrue();
            verify(userRepository).save(user);
            assertThat(resetToken.isUsed()).isTrue();
        }

        @Test
        @DisplayName("should throw when token not found")
        void shouldThrowWhenTokenNotFound() {
            ResetPasswordRequestDTO request = new ResetPasswordRequestDTO();
            request.setToken("invalid-token");
            request.setNewPassword("NewPassword123");

            when(passwordResetTokenRepository.findByToken("invalid-token"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.resetPassword(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("invalidOrExpiredToken");
        }

        @Test
        @DisplayName("should throw when token is already used")
        void shouldThrowWhenTokenAlreadyUsed() {
            ResetPasswordRequestDTO request = new ResetPasswordRequestDTO();
            request.setToken("used-token");
            request.setNewPassword("NewPassword123");

            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .token("used-token")
                    .user(user)
                    .expiresAt(LocalDateTime.now(Constants.TIMEZONE_ARGENTINA).plusHours(1))
                    .used(true)
                    .build();

            when(passwordResetTokenRepository.findByToken("used-token"))
                    .thenReturn(Optional.of(resetToken));

            assertThatThrownBy(() -> authService.resetPassword(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("invalidOrExpiredToken");
        }

        @Test
        @DisplayName("should throw when token is expired")
        void shouldThrowWhenTokenExpired() {
            ResetPasswordRequestDTO request = new ResetPasswordRequestDTO();
            request.setToken("expired-token");
            request.setNewPassword("NewPassword123");

            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .token("expired-token")
                    .user(user)
                    .expiresAt(LocalDateTime.now(Constants.TIMEZONE_ARGENTINA).minusHours(1))
                    .used(false)
                    .build();

            when(passwordResetTokenRepository.findByToken("expired-token"))
                    .thenReturn(Optional.of(resetToken));

            assertThatThrownBy(() -> authService.resetPassword(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("invalidOrExpiredToken");
        }
    }

    @Nested
    @DisplayName("processOAuth2Login")
    class ProcessOAuth2Login {

        @Test
        @DisplayName("should login existing provider user")
        void shouldLoginExistingProviderUser() {
            UserProvider userProvider = UserProvider.builder()
                    .user(user)
                    .provider(Provider.GOOGLE)
                    .providerId("google-456")
                    .build();

            when(userProviderRepository.findByProviderAndProviderId(Provider.GOOGLE, "google-456"))
                    .thenReturn(Optional.of(userProvider));
            when(tokenRepository.findAllByUserIdAndExpiredFalseAndRevokedFalse(any())).thenReturn(List.of());
            when(jwtService.generateToken(any())).thenReturn("accessToken");
            when(jwtService.generateRefreshToken(any())).thenReturn("refreshToken");
            when(tokenRepository.save(any())).thenReturn(null);

            ApiResponseDTO<AuthResponseDTO> response = authService.processOAuth2Login(
                    OAuth2LoginRequestDTO.builder()
                            .provider(Provider.GOOGLE)
                            .providerId("google-456")
                            .email("nuevo@test.com")
                            .firstName("Nuevo")
                            .lastName("Usuario")
                            .language("en")
                            .ip("127.0.0.1")
                            .userAgent("test-agent")
                            .build());

            assertThat(response.isSuccess()).isTrue();
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should create new user on first OAuth2 login")
        void shouldCreateNewUserOnFirstOAuth2Login() {
            when(userProviderRepository.findByProviderAndProviderId(Provider.GOOGLE, "google-456"))
                    .thenReturn(Optional.empty());
            when(userRepository.findByEmail("nuevo@test.com")).thenReturn(Optional.empty());
            when(userRepository.save(any(User.class))).thenReturn(user);
            when(userProviderRepository.save(any())).thenReturn(null);
            when(tokenRepository.findAllByUserIdAndExpiredFalseAndRevokedFalse(any())).thenReturn(List.of());
            when(jwtService.generateToken(any())).thenReturn("accessToken");
            when(jwtService.generateRefreshToken(any())).thenReturn("refreshToken");
            when(tokenRepository.save(any())).thenReturn(null);

            ApiResponseDTO<AuthResponseDTO> response = authService.processOAuth2Login(
                    OAuth2LoginRequestDTO.builder()
                            .provider(Provider.GOOGLE)
                            .providerId("google-456")
                            .email("nuevo@test.com")
                            .firstName("Nuevo")
                            .lastName("Usuario")
                            .language("en")
                            .ip("127.0.0.1")
                            .userAgent("test-agent")
                            .build());

            assertThat(response.isSuccess()).isTrue();
            verify(userRepository).save(any(User.class));
            verify(externalInvitationService).claimExternalInvitations(any(), eq("en"));
        }

        @Test
        @DisplayName("should link provider to existing user with same email")
        void shouldLinkProviderToExistingUserWithSameEmail() {
            when(userProviderRepository.findByProviderAndProviderId(Provider.GOOGLE, "google-456"))
                    .thenReturn(Optional.empty());
            when(userRepository.findByEmail("nuevo@test.com")).thenReturn(Optional.of(user));
            when(userProviderRepository.save(any())).thenReturn(null);
            when(tokenRepository.findAllByUserIdAndExpiredFalseAndRevokedFalse(any())).thenReturn(List.of());
            when(jwtService.generateToken(any())).thenReturn("accessToken");
            when(jwtService.generateRefreshToken(any())).thenReturn("refreshToken");
            when(tokenRepository.save(any())).thenReturn(null);

            ApiResponseDTO<AuthResponseDTO> response = authService.processOAuth2Login(
                    OAuth2LoginRequestDTO.builder()
                            .provider(Provider.GOOGLE)
                            .providerId("google-456")
                            .email("nuevo@test.com")
                            .firstName("Nuevo")
                            .lastName("Usuario")
                            .language("en")
                            .ip("127.0.0.1")
                            .userAgent("test-agent")
                            .build());

            assertThat(response.isSuccess()).isTrue();
            verify(userRepository, never()).save(any());
            verify(userProviderRepository).save(any(UserProvider.class));
        }
    }
}