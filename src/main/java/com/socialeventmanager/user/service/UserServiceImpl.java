package com.socialeventmanager.user.service;

import com.socialeventmanager.shared.dto.ApiResponseDTO;
import com.socialeventmanager.shared.exception.BadRequestException;
import com.socialeventmanager.user.dto.ChangePasswordRequestDTO;
import com.socialeventmanager.user.dto.UserResponseDTO;
import com.socialeventmanager.user.entity.User;
import com.socialeventmanager.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    @Override
    public ApiResponseDTO<UserResponseDTO> me(User user) {
        return new ApiResponseDTO<>(
                true,
                "User profile fetched successfully",
                UserResponseDTO.builder()
                        .id(user.getId())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .email(user.getEmail())
                        .build());
    }

    @Override
    public ApiResponseDTO<Void> changePassword(User user, ChangePasswordRequestDTO request) {
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("incorrectCurrentPassword");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BadRequestException("newPasswordSameAsCurrent");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return new ApiResponseDTO<>(true, "Password changed successfully", null);
    }
}