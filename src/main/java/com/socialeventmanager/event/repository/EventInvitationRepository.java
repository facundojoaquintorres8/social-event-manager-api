package com.socialeventmanager.event.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.socialeventmanager.event.entity.Event;
import com.socialeventmanager.event.entity.EventInvitation;
import com.socialeventmanager.event.enums.InvitationStatus;
import com.socialeventmanager.user.entity.User;

public interface EventInvitationRepository extends JpaRepository<EventInvitation, UUID>,
                JpaSpecificationExecutor<EventInvitation> {

    Page<EventInvitation> findAllByInvitedUser(User user, Pageable pageable);

    Optional<EventInvitation> findByEventAndInvitedUser(
            Event event,
            User invitedUser);

    Optional<EventInvitation> findByIdAndInvitedUser(
                    UUID id,
                    User invitedUser);

    Page<EventInvitation> findAllByEvent(
                    Event event,
                    Pageable pageable);

    Page<EventInvitation> findAllByInvitedUserAndStatus(
                    User invitedUser,
                    InvitationStatus status,
                    Pageable pageable);
}