package com.socialeventmanager.notification.consumer;

import com.socialeventmanager.kafka.event.EventCancelledEvent;
import com.socialeventmanager.kafka.event.EventReminderEvent;
import com.socialeventmanager.kafka.event.InvitationCreatedEvent;
import com.socialeventmanager.kafka.event.InvitationRespondedEvent;
import com.socialeventmanager.kafka.event.PasswordResetRequestedEvent;
import com.socialeventmanager.kafka.event.UserRegisteredEvent;
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

    @KafkaListener(topics = "user-registered", groupId = "notification-group")
    public void handleUserRegistered(UserRegisteredEvent event) {
        emailService.sendWelcomeEmail(event);
    }

    @KafkaListener(topics = "event-cancelled", groupId = "notification-group")
    public void handleEventCancelled(EventCancelledEvent event) {
        emailService.sendEventCancelledEmail(event);
    }

    @KafkaListener(topics = "invitation-responded", groupId = "notification-group")
    public void handleInvitationResponded(InvitationRespondedEvent event) {
        emailService.sendInvitationRespondedEmail(event);
    }

    @KafkaListener(topics = "event-reminder", groupId = "notification-group")
    public void handleEventReminder(EventReminderEvent event) {
        emailService.sendEventReminderEmail(event);
    }

    @KafkaListener(topics = "password-reset-requested", groupId = "notification-group")
    public void handlePasswordResetRequested(PasswordResetRequestedEvent event) {
        emailService.sendPasswordResetEmail(event);
    }
}