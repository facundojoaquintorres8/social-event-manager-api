package com.socialeventmanager.event.dto;

import com.socialeventmanager.event.enums.EventStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class EventDetailsFullResponseDTO {

    private UUID id;
    private String title;
    private String description;
    private LocalDateTime eventDate;
    private String location;
    private String locationAddress;
    private String placeId;
    private Double latitude;
    private Double longitude;
    private String createdBy;
    private EventStatus status;
    private List<EventParticipantResponseDTO> participants;
}