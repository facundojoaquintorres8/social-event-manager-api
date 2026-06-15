package com.socialeventmanager.shared.exception;

import com.socialeventmanager.shared.dto.ApiResponseDTO;
import io.jsonwebtoken.JwtException;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponseDTO<Void> handleBadRequest(BadRequestException ex) {
        return new ApiResponseDTO<>(
                false,
                ex.getMessage(),
                null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponseDTO<Void> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult()
                .getFieldError()
                .getDefaultMessage();

        return new ApiResponseDTO<>(
                false,
                message,
                null);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponseDTO<Void> handleGeneric(Exception ex) {
        return new ApiResponseDTO<>(
                false,
                "Internal server error",
                null);
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponseDTO<Void> handleBadCredentials(BadCredentialsException ex) {
        return new ApiResponseDTO<>(
                false,
                "Invalid email or password",
                null);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponseDTO<Void> handleUsernameNotFound(UsernameNotFoundException ex) {
        return new ApiResponseDTO<>(
                false,
                "Invalid email or password",
                null);
    }

    @ExceptionHandler(JwtException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponseDTO<Void> handleJwtException(JwtException ex) {
        return new ApiResponseDTO<>(
                false,
                "Invalid or expired token",
                null);
    }

    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponseDTO<Void> handleForbidden(ForbiddenException ex) {
        return new ApiResponseDTO<>(
                false,
                ex.getMessage(),
                null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponseDTO<Void> handleTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        if (UUID.class.equals(ex.getRequiredType())) {
            return new ApiResponseDTO<>(
                    false,
                    "Invalid ID format",
                    null);
        }
        return new ApiResponseDTO<>(
                false,
                "Invalid type format",
                null);
    }

}