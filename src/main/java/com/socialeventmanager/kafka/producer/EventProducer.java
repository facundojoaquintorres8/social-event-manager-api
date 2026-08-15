package com.socialeventmanager.kafka.producer;

import com.socialeventmanager.kafka.config.KafkaTopicConfig;
import com.socialeventmanager.kafka.event.EventCancelledEvent;
import com.socialeventmanager.kafka.event.EventReminderEvent;
import com.socialeventmanager.kafka.event.InvitationCreatedEvent;
import com.socialeventmanager.kafka.event.InvitationRespondedEvent;
import com.socialeventmanager.kafka.event.LoginAuditEvent;
import com.socialeventmanager.kafka.event.NotificationEvent;
import com.socialeventmanager.kafka.event.PasswordResetRequestedEvent;
import com.socialeventmanager.kafka.event.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendInvitationCreated(InvitationCreatedEvent event) {
        kafkaTemplate.send(KafkaTopicConfig.INVITATIONS_TOPIC, event)
                .whenComplete((result, ex) -> {
                    if (ex != null)
                        log.error("Failed to publish InvitationCreatedEvent for invitation {}: {}",
                                event.invitationId(), ex.getMessage());
                });
    }

    public void sendInvitationResponded(InvitationRespondedEvent event) {
        kafkaTemplate.send(KafkaTopicConfig.INVITATIONS_TOPIC, event)
                .whenComplete((result, ex) -> {
                    if (ex != null)
                        log.error("Failed to publish InvitationRespondedEvent for invitation {}: {}",
                                event.invitationId(), ex.getMessage());
                });
    }

    public void sendEventCancelled(EventCancelledEvent event) {
        kafkaTemplate.send(KafkaTopicConfig.EVENTS_TOPIC, event)
                .whenComplete((result, ex) -> {
                    if (ex != null)
                        log.error("Failed to publish EventCancelledEvent for event {}: {}",
                                event.eventId(), ex.getMessage());
                });
    }

    public void sendEventReminder(EventReminderEvent event) {
        kafkaTemplate.send(KafkaTopicConfig.EVENTS_TOPIC, event)
                .whenComplete((result, ex) -> {
                    if (ex != null)
                        log.error("Failed to publish EventReminderEvent for event {}: {}",
                                event.eventId(), ex.getMessage());
                });
    }

    public void sendUserRegistered(UserRegisteredEvent event) {
        kafkaTemplate.send(KafkaTopicConfig.USERS_TOPIC, event)
                .whenComplete((result, ex) -> {
                    if (ex != null)
                        log.error("Failed to publish UserRegisteredEvent for user {}: {}",
                                event.userId(), ex.getMessage());
                });
    }

    public void sendPasswordResetRequested(PasswordResetRequestedEvent event) {
        kafkaTemplate.send(KafkaTopicConfig.USERS_TOPIC, event)
                .whenComplete((result, ex) -> {
                    if (ex != null)
                        log.error("Failed to publish PasswordResetRequestedEvent for {}: {}",
                                event.email(), ex.getMessage());
                });
    }

    public void sendNotification(NotificationEvent event) {
        kafkaTemplate.send(KafkaTopicConfig.NOTIFICATIONS_TOPIC, event)
                .whenComplete((result, ex) -> {
                    if (ex != null)
                        log.error("Failed to publish NotificationEvent for event {}: {}",
                                event.eventId(), ex.getMessage());
                });
    }

    public void sendLoginAudit(LoginAuditEvent event) {
        kafkaTemplate.send(KafkaTopicConfig.AUDIT_TOPIC, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish LoginAuditEvent for {}: {}",
                                event.email(), ex.getMessage());
                    }
                });
    }

}