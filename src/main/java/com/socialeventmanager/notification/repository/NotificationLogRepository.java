package com.socialeventmanager.notification.repository;

import com.socialeventmanager.notification.entity.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, UUID> {
    boolean existsByInvitationId(UUID invitationId);
}