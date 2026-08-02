package com.socialeventmanager.user.controller;

import com.socialeventmanager.shared.dto.ApiResponseDTO;
import com.socialeventmanager.user.dto.ChangePasswordRequestDTO;
import com.socialeventmanager.user.dto.SetPasswordRequestDTO;
import com.socialeventmanager.user.dto.UserResponseDTO;
import com.socialeventmanager.user.entity.User;
import com.socialeventmanager.user.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponseDTO<UserResponseDTO>> me(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(userService.me(user));
    }

    @PutMapping("/change-password")
    public ResponseEntity<ApiResponseDTO<Void>> changePassword(
            @AuthenticationPrincipal User user,
            @RequestBody @Valid ChangePasswordRequestDTO request) {
        return ResponseEntity.ok(userService.changePassword(user, request));
    }

    @PutMapping("/set-password")
    public ResponseEntity<ApiResponseDTO<Void>> setPassword(
            @AuthenticationPrincipal User user,
            @RequestBody @Valid SetPasswordRequestDTO request) {
        return ResponseEntity.ok(userService.setPassword(user, request));
    }
}