package com.socialeventmanager.event.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.socialeventmanager.event.entity.Event;
import com.socialeventmanager.event.entity.ExternalInvitation;
import com.socialeventmanager.event.enums.ExternalInvitationStatus;

public interface ExternalInvitationRepository extends JpaRepository<ExternalInvitation, UUID> {

    Optional<ExternalInvitation> findByToken(String token);

    Optional<ExternalInvitation> findByEventAndInvitedEmail(
            Event event,
            String invitedEmail);

    List<ExternalInvitation> findAllByInvitedEmailAndStatus(
            String invitedEmail,
            ExternalInvitationStatus status);

    List<ExternalInvitation> findAllByEvent(Event event);

    List<ExternalInvitation> findAllByEventAndStatus(Event event, ExternalInvitationStatus pending);

    long countByEventAndStatus(Event event, ExternalInvitationStatus status);

}