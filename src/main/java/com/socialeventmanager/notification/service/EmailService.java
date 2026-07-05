package com.socialeventmanager.notification.service;

import com.socialeventmanager.kafka.event.EventCancelledEvent;
import com.socialeventmanager.kafka.event.EventReminderEvent;
import com.socialeventmanager.kafka.event.InvitationCreatedEvent;
import com.socialeventmanager.kafka.event.InvitationRespondedEvent;
import com.socialeventmanager.kafka.event.UserRegisteredEvent;

public interface EmailService {
    void sendInvitationEmail(InvitationCreatedEvent event);

    void sendWelcomeEmail(UserRegisteredEvent event);

    void sendEventCancelledEmail(EventCancelledEvent event);

    void sendInvitationRespondedEmail(InvitationRespondedEvent event);

    void sendEventReminderEmail(EventReminderEvent event);
}