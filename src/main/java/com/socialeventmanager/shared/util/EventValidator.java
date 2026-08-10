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
            throw new BadRequestException("eventCancelledCannotModify");
        }

        if (event.getEventDate().isBefore(LocalDateTime.now(Constants.TIMEZONE_ARGENTINA))) {
            throw new BadRequestException("eventPastCannotModify");
        }
    }

    public void validateEventAllowsContributions(Event event) {
        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new BadRequestException("eventCancelledCannotModify");
        }
    }
}