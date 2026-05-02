package com.socialeventmanager.event.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.socialeventmanager.event.enums.EventStatus;

import lombok.Builder;
import lombok.Data;

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