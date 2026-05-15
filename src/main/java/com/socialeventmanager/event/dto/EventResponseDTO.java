package com.socialeventmanager.event.dto;

import com.socialeventmanager.event.enums.EventStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class EventResponseDTO {

    private UUID id;
    private String title;
    private String description;
    private LocalDateTime eventDate;
    private String location;
    private String createdBy;
    private EventStatus status;
}