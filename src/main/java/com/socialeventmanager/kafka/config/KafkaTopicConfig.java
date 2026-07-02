package com.socialeventmanager.kafka.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String INVITATION_CREATED_TOPIC = "invitation-created";
    public static final String INVITATION_CREATED_DLT = "invitation-created-dlt";

    @Bean
    public NewTopic invitationCreatedTopic() {
        return TopicBuilder
                .name(INVITATION_CREATED_TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic invitationCreatedDltTopic() {
        return TopicBuilder
                .name(INVITATION_CREATED_DLT)
                .partitions(1)
                .replicas(1)
                .build();
    }
}