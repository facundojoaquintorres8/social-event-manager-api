package com.socialeventmanager.kafka.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String INVITATIONS_TOPIC = "invitations";
    public static final String EVENTS_TOPIC = "events";
    public static final String USERS_TOPIC = "users";
    public static final String NOTIFICATIONS_TOPIC = "notifications";
    public static final String AUDIT_TOPIC = "audit";

    @Bean
    public NewTopic invitationsTopic() {
        return TopicBuilder.name(INVITATIONS_TOPIC).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic eventsTopic() {
        return TopicBuilder.name(EVENTS_TOPIC).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic usersTopic() {
        return TopicBuilder.name(USERS_TOPIC).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic notificationsTopic() {
        return TopicBuilder.name(NOTIFICATIONS_TOPIC).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic auditTopic() {
        return TopicBuilder.name(AUDIT_TOPIC).partitions(1).replicas(1).build();
    }
}