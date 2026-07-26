package com.socialeventmanager.auth.controller;

import com.socialeventmanager.auth.dto.AuthResponseDTO;
import com.socialeventmanager.auth.dto.ForgotPasswordRequestDTO;
import com.socialeventmanager.auth.dto.LoginRequestDTO;
import com.socialeventmanager.auth.dto.RefreshRequestDTO;
import com.socialeventmanager.auth.dto.RegisterRequestDTO;
import com.socialeventmanager.auth.dto.ResetPasswordRequestDTO;
import com.socialeventmanager.auth.service.AuthService;
import com.socialeventmanager.shared.dto.ApiResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponseDTO<AuthResponseDTO>> register(
            @Valid @RequestBody RegisterRequestDTO request,
            @RequestHeader(value = "Accept-Language", defaultValue = "en") String language) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(authService.register(request, language));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponseDTO<AuthResponseDTO>> login(
            @Valid @RequestBody LoginRequestDTO request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponseDTO<AuthResponseDTO>> refreshToken(
            @Valid @RequestBody RefreshRequestDTO request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponseDTO<Void>> forgotPassword(
            @RequestBody @Valid ForgotPasswordRequestDTO request,
            @RequestHeader(value = "Accept-Language", defaultValue = "en") String acceptLanguage) {
        String language = acceptLanguage.split("[,;-]")[0].trim().toLowerCase();
        language = language.equals("es") ? "es" : "en";
        return ResponseEntity.ok(authService.forgotPassword(request, language));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponseDTO<Void>> resetPassword(
            @RequestBody @Valid ResetPasswordRequestDTO request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }

}