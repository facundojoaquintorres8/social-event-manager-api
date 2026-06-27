package com.socialeventmanager.event.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.socialeventmanager.event.enums.ExternalInvitationStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExternalInvitationPreviewResponseDTO {
    private UUID eventId;
    private String title;
    private String description;
    private LocalDateTime eventDate;
    private String location;
    private Double latitude;
    private Double longitude;
    private String createdBy;
    private ExternalInvitationStatus status;
    private boolean alreadyClaimed;
    private String invitedEmail;
}
