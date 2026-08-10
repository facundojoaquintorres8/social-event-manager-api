package com.socialeventmanager.notification.service;

import com.socialeventmanager.event.repository.EventRepository;
import com.socialeventmanager.notification.dto.NotificationResponseDTO;
import com.socialeventmanager.notification.entity.Notification;
import com.socialeventmanager.notification.enums.NotificationType;
import com.socialeventmanager.notification.repository.NotificationRepository;
import com.socialeventmanager.shared.dto.ApiResponseDTO;
import com.socialeventmanager.shared.exception.BadRequestException;
import com.socialeventmanager.shared.util.Constants;
import com.socialeventmanager.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    @Override
    @Transactional
    public void createNotifications(UUID eventId, NotificationType type,
            Map<String, String> params, List<UUID> recipientIds) {
        var event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BadRequestException("eventNotFound"));

        List<Notification> notifications = recipientIds.stream()
                .map(userId -> userRepository.findById(userId)
                        .map(user -> Notification.builder()
                                .user(user)
                                .event(event)
                                .type(type)
                                .params(params)
                                .build())
                        .orElse(null))
                .filter(n -> n != null)
                .toList();

        notificationRepository.saveAll(notifications);
    }

    @Override
    public List<Notification> getUnreadNotifications(UUID userId) {
        return notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId);
    }

    @Override
    public ApiResponseDTO<Page<NotificationResponseDTO>> getAllNotifications(UUID userId, int page, int size) {
        size = Math.min(size, 50);
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<NotificationResponseDTO> notifications = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::mapToDTO);
        return new ApiResponseDTO<>(true, "Notifications retrieved successfully", notifications);
    }

    @Override
    @Transactional
    public ApiResponseDTO<Void> markAsRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BadRequestException("notificationNotFound"));

        if (!notification.getUser().getId().equals(userId)) {
            throw new BadRequestException("notificationNotFound");
        }

        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now(Constants.TIMEZONE_ARGENTINA));
            notificationRepository.save(notification);
        }

        return new ApiResponseDTO<>(true, "Notification marked as read", null);
    }

    @Override
    @Transactional
    public ApiResponseDTO<Void> markAllAsRead(UUID userId) {
        notificationRepository.markAllAsReadByUserId(userId);
        return new ApiResponseDTO<>(true, "All notifications marked as read", null);
    }

    @Override
    public long countUnread(UUID userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    private NotificationResponseDTO mapToDTO(Notification notification) {
        return NotificationResponseDTO.builder()
                .id(notification.getId())
                .eventId(notification.getEvent().getId())
                .type(notification.getType())
                .params(notification.getParams())
                .read(notification.isRead())
                .readAt(notification.getReadAt())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}