package com.socialeventmanager.notification.service;

import com.socialeventmanager.kafka.event.InvitationCreatedEvent;
import com.socialeventmanager.kafka.event.UserRegisteredEvent;

public interface EmailService {
    void sendInvitationEmail(InvitationCreatedEvent event);

    void sendWelcomeEmail(UserRegisteredEvent event);
}