package com.socialeventmanager.event.repository;

import com.socialeventmanager.event.entity.Event;
import com.socialeventmanager.event.enums.EventStatus;
import com.socialeventmanager.user.entity.User;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.Locale;

public class EventSpecification {

    private EventSpecification() {
    }

    public static Specification<Event> hasUser(User user) {
        return (root, query, cb) -> cb.equal(root.get("createdBy"), user);
    }

    public static Specification<Event> titleContains(String title) {
        return (root, query, cb) -> cb.like(
                cb.lower(root.get("title")),
                "%" + title.toLowerCase(Locale.ROOT) + "%");
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

    public static Specification<Event> hasStatus(EventStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }
}