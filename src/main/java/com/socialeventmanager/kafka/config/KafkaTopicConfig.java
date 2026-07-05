package com.socialeventmanager.kafka.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String INVITATION_CREATED_TOPIC = "invitation-created";
    public static final String INVITATION_CREATED_DLT = "invitation-created-dlt";
    public static final String USER_REGISTERED_TOPIC = "user-registered";
    public static final String USER_REGISTERED_DLT = "user-registered-dlt";
    public static final String EVENT_CANCELLED_TOPIC = "event-cancelled";
    public static final String EVENT_CANCELLED_DLT = "event-cancelled-dlt";

    @Bean
    public NewTopic invitationCreatedTopic() {
        return TopicBuilder.name(INVITATION_CREATED_TOPIC).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic invitationCreatedDltTopic() {
        return TopicBuilder.name(INVITATION_CREATED_DLT).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic userRegisteredTopic() {
        return TopicBuilder.name(USER_REGISTERED_TOPIC).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic userRegisteredDltTopic() {
        return TopicBuilder.name(USER_REGISTERED_DLT).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic eventCancelledTopic() {
        return TopicBuilder.name(EVENT_CANCELLED_TOPIC).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic eventCancelledDltTopic() {
        return TopicBuilder.name(EVENT_CANCELLED_DLT).partitions(1).replicas(1).build();
    }
}