package com.socialeventmanager.config;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import jakarta.annotation.PostConstruct;

@Configuration
public class KafkaSSLConfig {

    @Value("${spring.kafka.properties.ssl.truststore.password}")
    private String truststorePassword;

    @PostConstruct
    public void configureTruststore() throws Exception {
        ClassPathResource resource = new ClassPathResource("certs/kafka.truststore.jks");
        if (resource.exists()) {
            File tempFile = File.createTempFile("kafka-truststore", ".jks");
            tempFile.deleteOnExit();
            Files.copy(resource.getInputStream(), tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.setProperty("spring.kafka.properties.ssl.truststore.location", tempFile.getAbsolutePath());
        }
    }
}