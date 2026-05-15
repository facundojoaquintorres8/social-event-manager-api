package com.socialeventmanager.user.service;

import com.socialeventmanager.shared.dto.ApiResponseDTO;
import com.socialeventmanager.user.dto.UserResponseDTO;
import com.socialeventmanager.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

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
                        .build()
        );
    }
}