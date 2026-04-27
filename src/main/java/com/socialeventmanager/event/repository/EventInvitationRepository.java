package com.socialeventmanager.event.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.socialeventmanager.event.entity.Event;
import com.socialeventmanager.event.entity.EventInvitation;
import com.socialeventmanager.user.entity.User;

public interface EventInvitationRepository
        extends JpaRepository<EventInvitation, UUID> {

    List<EventInvitation> findAllByInvitedUser(User user);

    Optional<EventInvitation> findByEventAndInvitedUser(
            Event event,
            User invitedUser);

    Optional<EventInvitation> findByIdAndInvitedUser(
                    UUID id,
                    User invitedUser);

    List<EventInvitation> findAllByEvent(Event event);
}