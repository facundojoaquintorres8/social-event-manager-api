package com.socialeventmanager.event.dto;

import com.socialeventmanager.event.enums.EventStatus;
import com.socialeventmanager.event.enums.InvitationStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class InvitationResponseDTO {

    private UUID invitationId;
    private UUID eventId;
    private String title;
    private LocalDateTime eventDate;
    private String location;
    private String locationAddress;
    private String placeId;
    private Double latitude;
    private Double longitude;
    private String createdBy;
    private InvitationStatus status;
    private EventStatus eventStatus;
}