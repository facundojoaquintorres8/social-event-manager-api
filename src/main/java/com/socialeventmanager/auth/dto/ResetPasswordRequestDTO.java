package com.socialeventmanager.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ResetPasswordRequestDTO {

    @NotBlank(message = "tokenRequired")
    private String token;

    @NotBlank(message = "newPasswordRequired")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$", message = "passwordInvalid")
    private String newPassword;
}
