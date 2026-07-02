package com.socialeventmanager.notification.service;

import java.util.UUID;

public interface NotificationLogService {
    boolean existsByInvitationId(UUID invitationId);

    void save(UUID invitationId);

    void deleteByInvitationId(UUID invitationId);
}
