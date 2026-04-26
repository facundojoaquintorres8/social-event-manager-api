package com.socialeventmanager.event.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.socialeventmanager.event.dto.CreateEventRequestDTO;
import com.socialeventmanager.event.dto.EventResponseDTO;
import com.socialeventmanager.event.service.EventService;
import com.socialeventmanager.shared.dto.ApiResponseDTO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @PostMapping
    public ResponseEntity<ApiResponseDTO<EventResponseDTO>> createEvent(
            @Valid @RequestBody CreateEventRequestDTO request) {
        return ResponseEntity.ok(eventService.createEvent(request));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponseDTO<List<EventResponseDTO>>> getMyEvents() {
        return ResponseEntity.ok(eventService.getMyEvents());
    }
}