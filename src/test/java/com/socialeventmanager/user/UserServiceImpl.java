package com.socialeventmanager.user;

import com.socialeventmanager.shared.dto.ApiResponseDTO;
import com.socialeventmanager.shared.exception.BadRequestException;
import com.socialeventmanager.user.dto.ChangePasswordRequestDTO;
import com.socialeventmanager.user.dto.SetPasswordRequestDTO;
import com.socialeventmanager.user.dto.UserResponseDTO;
import com.socialeventmanager.user.entity.User;
import com.socialeventmanager.user.repository.UserRepository;
import com.socialeventmanager.user.service.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl")
class UserServiceImplTest {

    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;

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
    }

    @Nested
    @DisplayName("me")
    class Me {

        @Test
        @DisplayName("should return user profile")
        void shouldReturnUserProfile() {
            ApiResponseDTO<UserResponseDTO> response = userService.me(user);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData().getEmail()).isEqualTo("facundo@test.com");
            assertThat(response.getData().getFirstName()).isEqualTo("Facundo");
            assertThat(response.getData().getLastName()).isEqualTo("Torres");
            assertThat(response.getData().isHasPassword()).isTrue();
        }
    }

    @Nested
    @DisplayName("changePassword")
    class ChangePassword {

        @Test
        @DisplayName("should change password successfully")
        void shouldChangePasswordSuccessfully() {
            ChangePasswordRequestDTO request = new ChangePasswordRequestDTO();
            request.setCurrentPassword("currentPassword");
            request.setNewPassword("NewPassword1");

            when(passwordEncoder.matches("currentPassword", "encodedPassword")).thenReturn(true);
            when(passwordEncoder.matches("NewPassword1", "encodedPassword")).thenReturn(false);
            when(passwordEncoder.encode("NewPassword1")).thenReturn("newEncodedPassword");

            ApiResponseDTO<Void> response = userService.changePassword(user, request);

            assertThat(response.isSuccess()).isTrue();
            assertThat(user.getPassword()).isEqualTo("newEncodedPassword");
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("should throw when current password is incorrect")
        void shouldThrowWhenCurrentPasswordIsIncorrect() {
            ChangePasswordRequestDTO request = new ChangePasswordRequestDTO();
            request.setCurrentPassword("wrongPassword");
            request.setNewPassword("NewPassword1");

            when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

            assertThatThrownBy(() -> userService.changePassword(user, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("incorrectCurrentPassword");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw when new password is same as current")
        void shouldThrowWhenNewPasswordIsSameAsCurrent() {
            ChangePasswordRequestDTO request = new ChangePasswordRequestDTO();
            request.setCurrentPassword("currentPassword");
            request.setNewPassword("currentPassword");

            when(passwordEncoder.matches("currentPassword", "encodedPassword")).thenReturn(true);
            when(passwordEncoder.matches("currentPassword", "encodedPassword")).thenReturn(true);

            assertThatThrownBy(() -> userService.changePassword(user, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("newPasswordSameAsCurrent");

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("setPassword")
    class SetPassword {

        @Test
        @DisplayName("should set password successfully for OAuth2 user")
        void shouldSetPasswordSuccessfullyForOAuth2User() {
            user.setPassword(null);
            user.setHasPassword(false);

            SetPasswordRequestDTO request = new SetPasswordRequestDTO();
            request.setNewPassword("NewPassword1");

            when(passwordEncoder.encode("NewPassword1")).thenReturn("newEncodedPassword");

            ApiResponseDTO<Void> response = userService.setPassword(user, request);

            assertThat(response.isSuccess()).isTrue();
            assertThat(user.getPassword()).isEqualTo("newEncodedPassword");
            assertThat(user.isHasPassword()).isTrue();
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("should throw when user already has password")
        void shouldThrowWhenUserAlreadyHasPassword() {
            SetPasswordRequestDTO request = new SetPasswordRequestDTO();
            request.setNewPassword("NewPassword1");

            assertThatThrownBy(() -> userService.setPassword(user, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("passwordAlreadySet");

            verify(userRepository, never()).save(any());
        }
    }
}