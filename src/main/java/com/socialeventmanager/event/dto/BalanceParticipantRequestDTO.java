package com.socialeventmanager.event.dto;

import java.util.UUID;

import lombok.Data;

@Data
public class BalanceParticipantRequestDTO {
    private UUID userId;
    private String name;
}