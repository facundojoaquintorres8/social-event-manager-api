package com.socialeventmanager.user.service;

import com.socialeventmanager.shared.dto.ApiResponseDTO;
import com.socialeventmanager.user.dto.ChangePasswordRequestDTO;
import com.socialeventmanager.user.dto.UserResponseDTO;
import com.socialeventmanager.user.entity.User;

public interface UserService {
    ApiResponseDTO<UserResponseDTO> me(User user);

    ApiResponseDTO<Void> changePassword(User user, ChangePasswordRequestDTO request);
}