package com.socialeventmanager.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CreatePreferenceResponseDTO {
    private String preferenceId;
    private String initPoint;
    private String sandboxInitPoint;
    private String publicKey;
}