package com.socialeventmanager.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notification_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "invitation_id", nullable = false, unique = true)
    private UUID invitationId;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;
}