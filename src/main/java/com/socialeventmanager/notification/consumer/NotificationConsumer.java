package com.socialeventmanager.notification.consumer;

import com.socialeventmanager.kafka.event.InvitationCreatedEvent;
import com.socialeventmanager.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final EmailService emailService;

    @KafkaListener(topics = "invitation-created", groupId = "notification-group")
    public void handleInvitationCreated(InvitationCreatedEvent event) {
        emailService.sendInvitationEmail(event);
    }
}