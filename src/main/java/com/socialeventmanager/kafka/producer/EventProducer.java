package com.socialeventmanager.kafka.producer;

import com.socialeventmanager.kafka.event.InvitationCreatedEvent;
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
        kafkaTemplate.send("invitation-created", event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish InvitationCreatedEvent for invitation {}: {}",
                                event.invitationId(), ex.getMessage());
                    }
                });
    }

    public void sendUserRegistered(UserRegisteredEvent event) {
        kafkaTemplate.send("user-registered", event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish UserRegisteredEvent for user {}: {}",
                                event.userId(), ex.getMessage());
                    }
                });
    }
}