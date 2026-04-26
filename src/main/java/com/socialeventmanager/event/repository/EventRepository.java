package com.socialeventmanager.event.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.socialeventmanager.event.entity.Event;
import com.socialeventmanager.user.entity.User;

public interface EventRepository extends JpaRepository<Event, UUID> {

    List<Event> findAllByCreatedBy(User user);
}