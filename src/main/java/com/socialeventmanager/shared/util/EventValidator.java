package com.socialeventmanager.shared.util;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.socialeventmanager.event.entity.Event;
import com.socialeventmanager.event.enums.EventStatus;
import com.socialeventmanager.shared.exception.BadRequestException;

@Component
public class EventValidator {

    public void validateEventAllowsInteraction(Event event) {

        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new BadRequestException(
                    "Cancelled events cannot be modified");
        }

        if (event.getEventDate().isBefore(LocalDateTime.now())) {
            throw new BadRequestException(
                    "Past events cannot be modified");
        }
    }
}