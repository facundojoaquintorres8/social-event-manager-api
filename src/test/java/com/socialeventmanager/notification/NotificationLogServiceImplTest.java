package com.socialeventmanager.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.socialeventmanager.notification.repository.NotificationLogRepository;
import com.socialeventmanager.notification.service.NotificationLogServiceImpl;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationLogServiceImpl")
class NotificationLogServiceImplTest {

    @Mock
    private NotificationLogRepository notificationLogRepository;

    @InjectMocks
    private NotificationLogServiceImpl notificationLogService;

    private UUID invitationId;

    @BeforeEach
    void setUp() {
        invitationId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("existsByInvitationId")
    class ExistsByInvitationId {

        @Test
        @DisplayName("should return true when log exists")
        void shouldReturnTrueWhenLogExists() {
            when(notificationLogRepository.existsByInvitationId(invitationId)).thenReturn(true);

            boolean result = notificationLogService.existsByInvitationId(invitationId);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false when log does not exist")
        void shouldReturnFalseWhenLogDoesNotExist() {
            when(notificationLogRepository.existsByInvitationId(invitationId)).thenReturn(false);

            boolean result = notificationLogService.existsByInvitationId(invitationId);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("should save notification log")
        void shouldSaveNotificationLog() {
            notificationLogService.save(invitationId);

            verify(notificationLogRepository).save(argThat(log -> log.getInvitationId().equals(invitationId) &&
                    log.getSentAt() != null));
        }
    }

    @Nested
    @DisplayName("deleteByInvitationId")
    class DeleteByInvitationId {

        @Test
        @DisplayName("should delete notification log by invitation id")
        void shouldDeleteNotificationLogByInvitationId() {
            notificationLogService.deleteByInvitationId(invitationId);

            verify(notificationLogRepository).deleteByInvitationId(invitationId);
        }
    }
}