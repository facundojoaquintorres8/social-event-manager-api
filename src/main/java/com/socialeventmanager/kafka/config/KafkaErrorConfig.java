package com.socialeventmanager.kafka.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class KafkaErrorConfig {

    @Bean
    public DefaultErrorHandler errorHandler() {
        FixedBackOff backOff = new FixedBackOff(1000L, 3L);
        return new DefaultErrorHandler(
                (consumerRecord, ex) -> log.error("Message failed after retries. Topic: {}, Error: {}",
                        consumerRecord.topic(), ex.getMessage()),
                backOff);
    }
}