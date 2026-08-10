package com.socialeventmanager.notification.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.socialeventmanager.notification.entity.NotificationLog;
import com.socialeventmanager.notification.repository.NotificationLogRepository;
import com.socialeventmanager.shared.util.Constants;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationLogServiceImpl implements NotificationLogService {

    private final NotificationLogRepository notificationLogRepository;

    @Override
    public boolean existsByInvitationId(UUID invitationId) {
        return notificationLogRepository.existsByInvitationId(invitationId);
    }

    @Override
    public void save(UUID invitationId) {
        notificationLogRepository.save(
                NotificationLog.builder()
                        .invitationId(invitationId)
                        .sentAt(LocalDateTime.now(Constants.TIMEZONE_ARGENTINA))
                        .build());
    }

    @Override
    public void deleteByInvitationId(UUID invitationId) {
        notificationLogRepository.deleteByInvitationId(invitationId);
    }

}
