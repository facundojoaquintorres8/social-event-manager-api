package com.socialeventmanager.event.repository;

import java.time.LocalDateTime;

import org.springframework.data.jpa.domain.Specification;

import com.socialeventmanager.event.entity.Event;
import com.socialeventmanager.event.enums.EventStatus;
import com.socialeventmanager.user.entity.User;

public class EventSpecification {

    public static Specification<Event> hasUser(User user) {
        return (root, query, cb) -> cb.equal(root.get("createdBy"), user);
    }

    public static Specification<Event> titleContains(String title) {
        return (root, query, cb) -> cb.like(
                cb.lower(root.get("title")),
                "%" + title.toLowerCase() + "%");
    }

    public static Specification<Event> dateAfter(LocalDateTime fromDate) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(
                root.get("eventDate"),
                fromDate);
    }

    public static Specification<Event> dateBefore(LocalDateTime toDate) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(
                root.get("eventDate"),
                toDate);
    }

    public static Specification<Event> isActive() {
        return (root, query, cb) -> cb.equal(root.get("status"), EventStatus.ACTIVE);
    }
}