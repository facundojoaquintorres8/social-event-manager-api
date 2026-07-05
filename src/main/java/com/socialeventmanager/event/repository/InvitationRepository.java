package com.socialeventmanager.event.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.socialeventmanager.event.entity.Event;
import com.socialeventmanager.event.entity.EventInvitation;
import com.socialeventmanager.event.enums.InvitationStatus;
import com.socialeventmanager.user.entity.User;

public interface InvitationRepository extends JpaRepository<EventInvitation, UUID>,
        JpaSpecificationExecutor<EventInvitation> {

    Page<EventInvitation> findAllByInvitedUser(User user, Pageable pageable);

    Optional<EventInvitation> findByEventAndInvitedUser(
            Event event,
            User invitedUser);

    Optional<EventInvitation> findByEventIdAndInvitedUser(
            UUID eventId,
            User invitedUser);

    Page<EventInvitation> findAllByEvent(
            Event event,
            Pageable pageable);

    Page<EventInvitation> findAllByInvitedUserAndStatus(
            User invitedUser,
            InvitationStatus status,
            Pageable pageable);

    List<EventInvitation> findAllByEvent(Event event);

    List<EventInvitation> findAllByEventAndStatusNot(
            Event event,
            InvitationStatus status);

    boolean existsByEventIdAndInvitedUserAndStatusNot(
            UUID eventId,
            User invitedUser,
            InvitationStatus status);

    List<EventInvitation> findAllByInvitedUserAndStatusInAndEvent_EventDateBetween(
            User invitedUser,
            List<InvitationStatus> statuses,
            LocalDateTime from,
            LocalDateTime to);

    @EntityGraph(attributePaths = { "event", "invitedBy" })
    List<EventInvitation> findTop5ByInvitedUserIdAndStatusNotOrderByCreatedAtDesc(
            UUID invitedUserId,
            InvitationStatus status);

    List<EventInvitation> findAllByEventAndStatus(Event event, InvitationStatus status);
}