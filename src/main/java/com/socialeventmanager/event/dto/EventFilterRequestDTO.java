package com.socialeventmanager.event.dto;

import java.time.LocalDateTime;

import com.socialeventmanager.event.enums.EventStatus;

import lombok.Data;

@Data
public class EventFilterRequestDTO {
    private int page;
    private int size;
    private String sortBy;
    private String direction;
    private String title;
    private LocalDateTime fromDate;
    private LocalDateTime toDate;
    private EventStatus status;
}
