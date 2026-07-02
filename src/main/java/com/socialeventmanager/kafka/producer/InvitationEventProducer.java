package com.socialeventmanager.kafka.producer;

import com.socialeventmanager.kafka.config.KafkaTopicConfig;
import com.socialeventmanager.kafka.event.InvitationCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvitationEventProducer {

    private final KafkaTemplate<String, InvitationCreatedEvent> kafkaTemplate;

    public void sendInvitationCreated(InvitationCreatedEvent event) {
        kafkaTemplate.send(KafkaTopicConfig.INVITATION_CREATED_TOPIC, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish InvitationCreatedEvent for invitation {}: {}",
                                event.invitationId(), ex.getMessage());
                    }
                });
    }
}