package com.socialeventmanager.notification.controller;

import com.socialeventmanager.auth.service.JwtService;
import com.socialeventmanager.config.CustomUserDetailsService;
import com.socialeventmanager.notification.dto.NotificationResponseDTO;
import com.socialeventmanager.notification.service.NotificationService;
import com.socialeventmanager.notification.service.SseService;
import com.socialeventmanager.shared.dto.ApiResponseDTO;
import com.socialeventmanager.shared.exception.BadRequestException;
import com.socialeventmanager.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final SseService sseService;
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam String token) {
        try {
            String email = jwtService.extractUsername(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            if (!jwtService.isTokenValid(token, userDetails)) {
                throw new BadRequestException("invalidOrExpiredToken");
            }

            User user = (User) userDetails;
            return sseService.subscribe(user.getId());
        } catch (Exception e) {
            throw new BadRequestException("invalidOrExpiredToken");
        }
    }

    @GetMapping("/unread")
    public ResponseEntity<ApiResponseDTO<?>> getUnread(@AuthenticationPrincipal User user) {
        var notifications = notificationService.getUnreadNotifications(user.getId())
                .stream()
                .map(n -> NotificationResponseDTO.builder()
                        .id(n.getId())
                        .eventId(n.getEvent().getId())
                        .type(n.getType())
                        .params(n.getParams())
                        .read(n.isRead())
                        .readAt(n.getReadAt())
                        .createdAt(n.getCreatedAt())
                        .build())
                .toList();
        return ResponseEntity.ok(new ApiResponseDTO<>(true, "Unread notifications retrieved", notifications));
    }

    @GetMapping
    public ResponseEntity<ApiResponseDTO<Page<NotificationResponseDTO>>> getAll(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(notificationService.getAllNotifications(user.getId(), page, size));
    }

    @GetMapping("/unread/count")
    public ResponseEntity<ApiResponseDTO<Long>> countUnread(@AuthenticationPrincipal User user) {
        return ResponseEntity
                .ok(new ApiResponseDTO<>(true, "Unread count", notificationService.countUnread(user.getId())));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponseDTO<Void>> markAsRead(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(notificationService.markAsRead(id, user.getId()));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponseDTO<Void>> markAllAsRead(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(notificationService.markAllAsRead(user.getId()));
    }
}