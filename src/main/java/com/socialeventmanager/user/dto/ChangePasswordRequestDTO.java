package com.socialeventmanager.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ChangePasswordRequestDTO {

    @NotBlank(message = "currentPasswordRequired")
    private String currentPassword;

    @NotBlank(message = "newPasswordRequired")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$", message = "passwordInvalid")
    private String newPassword;
}
