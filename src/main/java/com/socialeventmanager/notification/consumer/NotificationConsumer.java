package com.socialeventmanager.notification.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.socialeventmanager.audit.service.AuditService;
import com.socialeventmanager.kafka.event.EventCancelledEvent;
import com.socialeventmanager.kafka.event.EventReminderEvent;
import com.socialeventmanager.kafka.event.InvitationCreatedEvent;
import com.socialeventmanager.kafka.event.InvitationRespondedEvent;
import com.socialeventmanager.kafka.event.LoginAuditEvent;
import com.socialeventmanager.kafka.event.NotificationEvent;
import com.socialeventmanager.kafka.event.PasswordResetRequestedEvent;
import com.socialeventmanager.kafka.event.UserRegisteredEvent;
import com.socialeventmanager.notification.service.EmailService;
import com.socialeventmanager.notification.service.NotificationService;
import com.socialeventmanager.notification.service.SseService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final EmailService emailService;
    private final NotificationService notificationService;
    private final SseService sseService;
    private final AuditService auditService;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @KafkaListener(topics = "invitations", groupId = "notification-group")
    public void handleInvitations(ConsumerRecord<String, String> consumerRecord) {
        try {
            String typeHeader = getSimpleTypeName(consumerRecord);
            if ("InvitationCreatedEvent".equals(typeHeader)) {
                InvitationCreatedEvent event = objectMapper.readValue(consumerRecord.value(),
                        InvitationCreatedEvent.class);
                emailService.sendInvitationEmail(event);
            } else if ("InvitationRespondedEvent".equals(typeHeader)) {
                InvitationRespondedEvent event = objectMapper.readValue(consumerRecord.value(),
                        InvitationRespondedEvent.class);
                emailService.sendInvitationRespondedEmail(event);
            }
        } catch (Exception e) {
            log.error("Failed to process invitation event: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "events", groupId = "notification-group")
    public void handleEvents(ConsumerRecord<String, String> consumerRecord) {
        try {
            String typeHeader = getSimpleTypeName(consumerRecord);
            if ("EventCancelledEvent".equals(typeHeader)) {
                EventCancelledEvent event = objectMapper.readValue(consumerRecord.value(), EventCancelledEvent.class);
                emailService.sendEventCancelledEmail(event);
            } else if ("EventReminderEvent".equals(typeHeader)) {
                EventReminderEvent event = objectMapper.readValue(consumerRecord.value(), EventReminderEvent.class);
                emailService.sendEventReminderEmail(event);
            }
        } catch (Exception e) {
            log.error("Failed to process event: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "users", groupId = "notification-group")
    public void handleUsers(ConsumerRecord<String, String> consumerRecord) {
        try {
            String typeHeader = getSimpleTypeName(consumerRecord);
            if ("UserRegisteredEvent".equals(typeHeader)) {
                UserRegisteredEvent event = objectMapper.readValue(consumerRecord.value(), UserRegisteredEvent.class);
                emailService.sendWelcomeEmail(event);
            } else if ("PasswordResetRequestedEvent".equals(typeHeader)) {
                PasswordResetRequestedEvent event = objectMapper.readValue(consumerRecord.value(),
                        PasswordResetRequestedEvent.class);
                emailService.sendPasswordResetEmail(event);
            }
        } catch (Exception e) {
            log.error("Failed to process user event: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "notifications", groupId = "notification-group")
    public void handleNotification(ConsumerRecord<String, String> consumerRecord) {
        try {
            NotificationEvent event = objectMapper.readValue(consumerRecord.value(), NotificationEvent.class);
            notificationService.createNotifications(
                    event.eventId(),
                    event.type(),
                    event.params(),
                    event.recipientIds());
            sseService.sendToUsers(event.recipientIds(), event.eventId(), event.type(), event.params());
        } catch (Exception e) {
            log.error("Failed to process notification event: {}", e.getMessage());
        }
    }

    private String getSimpleTypeName(ConsumerRecord<String, String> consumerRecord) {
        Header typeHeader = consumerRecord.headers().lastHeader("spring_json_header_types");
        if (typeHeader == null) {
            typeHeader = consumerRecord.headers().lastHeader("__TypeId__");
        }

        if (typeHeader == null) {
            return null;
        }

        String fullType = new String(typeHeader.value());
        return fullType.substring(fullType.lastIndexOf('.') + 1);
    }

    @KafkaListener(topics = "audit", groupId = "notification-group")
    public void handleAudit(ConsumerRecord<String, String> consumerRecord) {
        try {
            LoginAuditEvent event = objectMapper.readValue(consumerRecord.value(), LoginAuditEvent.class);
            auditService.saveLoginAudit(event);
        } catch (Exception e) {
            log.error("Failed to process audit event: {}", e.getMessage());
        }
    }
}