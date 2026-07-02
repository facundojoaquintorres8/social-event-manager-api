package com.socialeventmanager.notification.service;

import com.socialeventmanager.kafka.event.InvitationCreatedEvent;

public interface EmailService {
    void sendInvitationEmail(InvitationCreatedEvent event);
}