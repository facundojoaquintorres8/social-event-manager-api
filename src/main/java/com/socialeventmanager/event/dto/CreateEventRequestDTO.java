package com.socialeventmanager.event.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateEventRequestDTO {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "Event date is required")
    private LocalDateTime eventDate;

    @NotBlank(message = "Location is required")
    private String location;
}