package com.socialeventmanager.event.repository;

import com.socialeventmanager.event.entity.Event;
import com.socialeventmanager.event.enums.EventStatus;
import com.socialeventmanager.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID>,
        JpaSpecificationExecutor<Event> {

    Page<Event> findAllByCreatedBy(User user, Pageable pageable);

    List<Event> findAllByCreatedByAndStatusNotAndEventDateBetween(
            User createdBy,
            EventStatus status,
            LocalDateTime from,
            LocalDateTime to);

    Optional<Event> findByIdAndCreatedBy(UUID id, User user);

    long countByCreatedById(UUID createdById);

    long countByCreatedByIdAndStatus(
            UUID createdById,
            EventStatus status);

    long countByCreatedByIdAndEventDateAfter(
            UUID createdById,
            LocalDateTime now);

    List<Event> findTop5ByCreatedByIdOrderByCreatedAtDesc(
            UUID createdById);
}