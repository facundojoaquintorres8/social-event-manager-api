package com.socialeventmanager.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.socialeventmanager.event.entity.Event;
import com.socialeventmanager.event.enums.EventStatus;
import com.socialeventmanager.event.repository.EventRepository;
import com.socialeventmanager.notification.dto.NotificationResponseDTO;
import com.socialeventmanager.notification.entity.Notification;
import com.socialeventmanager.notification.enums.NotificationType;
import com.socialeventmanager.notification.repository.NotificationRepository;
import com.socialeventmanager.notification.service.NotificationServiceImpl;
import com.socialeventmanager.shared.dto.ApiResponseDTO;
import com.socialeventmanager.shared.exception.BadRequestException;
import com.socialeventmanager.shared.util.Constants;
import com.socialeventmanager.user.entity.User;
import com.socialeventmanager.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationServiceImpl")
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private User user;
    private Event event;
    private UUID userId;
    private UUID eventId;
    private UUID notificationId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        eventId = UUID.randomUUID();
        notificationId = UUID.randomUUID();

        user = User.builder()
                .firstName("Facundo")
                .lastName("Torres")
                .email("facundo@test.com")
                .hasPassword(true)
                .build();
        user.setId(userId);

        event = Event.builder()
                .title("Test Event")
                .eventDate(LocalDateTime.now(Constants.TIMEZONE_ARGENTINA).plusDays(1))
                .location("Test Location")
                .locationAddress("Test Address")
                .placeId("test-place-id")
                .latitude(-32.9)
                .longitude(-60.6)
                .createdBy(user)
                .status(EventStatus.ACTIVE)
                .language("en")
                .build();
        event.setId(eventId);
    }

    @Nested
    @DisplayName("createNotifications")
    class CreateNotifications {

        @Test
        @DisplayName("should create notifications for all recipients")
        void shouldCreateNotificationsForAllRecipients() {
            UUID recipientId1 = UUID.randomUUID();
            UUID recipientId2 = UUID.randomUUID();

            User recipient1 = User.builder().email("r1@test.com").build();
            recipient1.setId(recipientId1);
            User recipient2 = User.builder().email("r2@test.com").build();
            recipient2.setId(recipientId2);

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
            when(userRepository.findById(recipientId1)).thenReturn(Optional.of(recipient1));
            when(userRepository.findById(recipientId2)).thenReturn(Optional.of(recipient2));

            notificationService.createNotifications(
                    eventId,
                    NotificationType.INVITATION_RECEIVED,
                    Map.of("eventTitle", "Test Event"),
                    List.of(recipientId1, recipientId2));

            verify(notificationRepository).saveAll(argThat(list -> ((List<Notification>) list).size() == 2));
        }

        @Test
        @DisplayName("should skip non-existent users")
        void shouldSkipNonExistentUsers() {
            UUID existingId = UUID.randomUUID();
            UUID nonExistingId = UUID.randomUUID();

            User existingUser = User.builder().email("existing@test.com").build();
            existingUser.setId(existingId);

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
            when(userRepository.findById(existingId)).thenReturn(Optional.of(existingUser));
            when(userRepository.findById(nonExistingId)).thenReturn(Optional.empty());

            notificationService.createNotifications(
                    eventId,
                    NotificationType.EVENT_EDITED,
                    Map.of("eventTitle", "Test Event"),
                    List.of(existingId, nonExistingId));

            verify(notificationRepository).saveAll(argThat(list -> ((List<Notification>) list).size() == 1));
        }

        @Test
        @DisplayName("should throw when event not found")
        void shouldThrowWhenEventNotFound() {
            when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

            Map<String, String> params = Map.of();
            List<UUID> recipients = List.of(userId);

            assertThatThrownBy(() -> notificationService.createNotifications(
                    eventId, NotificationType.EVENT_EDITED,
                    params, recipients))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("eventNotFound");
        }
    }

    @Nested
    @DisplayName("markAsRead")
    class MarkAsRead {

        @Test
        @DisplayName("should mark notification as read")
        void shouldMarkNotificationAsRead() {
            Notification notification = Notification.builder()
                    .user(user)
                    .event(event)
                    .type(NotificationType.INVITATION_RECEIVED)
                    .params(Map.of("eventTitle", "Test Event"))
                    .read(false)
                    .build();
            notification.setId(notificationId);

            when(notificationRepository.findById(notificationId))
                    .thenReturn(Optional.of(notification));

            ApiResponseDTO<Void> response = notificationService.markAsRead(notificationId, userId);

            assertThat(response.isSuccess()).isTrue();
            assertThat(notification.isRead()).isTrue();
            assertThat(notification.getReadAt()).isNotNull();
            verify(notificationRepository).save(notification);
        }

        @Test
        @DisplayName("should not save when notification already read")
        void shouldNotSaveWhenNotificationAlreadyRead() {
            Notification notification = Notification.builder()
                    .user(user)
                    .event(event)
                    .type(NotificationType.INVITATION_RECEIVED)
                    .params(Map.of("eventTitle", "Test Event"))
                    .read(true)
                    .readAt(LocalDateTime.now(Constants.TIMEZONE_ARGENTINA))
                    .build();
            notification.setId(notificationId);

            when(notificationRepository.findById(notificationId))
                    .thenReturn(Optional.of(notification));

            notificationService.markAsRead(notificationId, userId);

            verify(notificationRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw when notification not found")
        void shouldThrowWhenNotificationNotFound() {
            when(notificationRepository.findById(notificationId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> notificationService.markAsRead(notificationId, userId))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("notificationNotFound");
        }

        @Test
        @DisplayName("should throw when notification belongs to different user")
        void shouldThrowWhenNotificationBelongsToDifferentUser() {
            User otherUser = User.builder().email("other@test.com").build();
            otherUser.setId(UUID.randomUUID());

            Notification notification = Notification.builder()
                    .user(otherUser)
                    .event(event)
                    .type(NotificationType.INVITATION_RECEIVED)
                    .params(Map.of())
                    .read(false)
                    .build();
            notification.setId(notificationId);

            when(notificationRepository.findById(notificationId))
                    .thenReturn(Optional.of(notification));

            assertThatThrownBy(() -> notificationService.markAsRead(notificationId, userId))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("notificationNotFound");
        }
    }

    @Nested
    @DisplayName("markAllAsRead")
    class MarkAllAsRead {

        @Test
        @DisplayName("should mark all notifications as read")
        void shouldMarkAllNotificationsAsRead() {
            ApiResponseDTO<Void> response = notificationService.markAllAsRead(userId);

            assertThat(response.isSuccess()).isTrue();
            verify(notificationRepository).markAllAsReadByUserId(userId);
        }
    }

    @Nested
    @DisplayName("countUnread")
    class CountUnread {

        @Test
        @DisplayName("should return unread count")
        void shouldReturnUnreadCount() {
            when(notificationRepository.countByUserIdAndReadFalse(userId)).thenReturn(5L);

            long count = notificationService.countUnread(userId);

            assertThat(count).isEqualTo(5L);
        }
    }

    @Nested
    @DisplayName("getAllNotifications")
    class GetAllNotifications {

        @Test
        @DisplayName("should return paginated notifications")
        void shouldReturnPaginatedNotifications() {
            Notification notification = Notification.builder()
                    .user(user)
                    .event(event)
                    .type(NotificationType.EVENT_EDITED)
                    .params(Map.of("eventTitle", "Test Event"))
                    .read(false)
                    .build();
            notification.setId(notificationId);

            Page<Notification> page = new PageImpl<>(List.of(notification));

            when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(userId), any(PageRequest.class)))
                    .thenReturn(page);

            ApiResponseDTO<Page<NotificationResponseDTO>> response = notificationService.getAllNotifications(userId, 0,
                    20);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData().getContent()).hasSize(1);
            assertThat(response.getData().getContent().get(0).getType())
                    .isEqualTo(NotificationType.EVENT_EDITED);
        }
    }
}