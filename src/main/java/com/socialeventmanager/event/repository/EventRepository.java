package com.socialeventmanager.event.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.socialeventmanager.event.entity.Event;
import com.socialeventmanager.user.entity.User;

public interface EventRepository extends JpaRepository<Event, UUID> {

    Page<Event> findAllByCreatedBy(User user, Pageable pageable);

    Optional<Event> findByIdAndCreatedBy(UUID id, User user);
}