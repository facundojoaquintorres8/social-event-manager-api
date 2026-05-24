package com.socialeventmanager.event.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.socialeventmanager.event.enums.EventStatus;
import com.socialeventmanager.event.enums.InvitationStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CalendarEventResponseDTO {
    private UUID id;
    private String title;
    private LocalDateTime eventDate;
    private String location;
    private EventStatus eventStatus;
    private InvitationStatus invitationStatus;
    private boolean owner;
}
