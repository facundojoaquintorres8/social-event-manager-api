package com.socialeventmanager.event.dto;

import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EventParticipantResponseDTO {

    private UUID userId;
    private String firstName;
    private String lastName;
    private String email;
    private String status;
    private boolean external;
}