package com.socialeventmanager.event.dto;

import com.socialeventmanager.event.enums.InvitationStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EventParticipantResponseDTO {

    private String firstName;
    private String lastName;
    private String email;
    private InvitationStatus status;
}