package com.socialeventmanager.notification.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.socialeventmanager.kafka.event.InvitationCreatedEvent;
import com.socialeventmanager.notification.entity.NotificationLog;
import com.socialeventmanager.notification.repository.NotificationLogRepository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final NotificationLogRepository notificationLogRepository;

    @Value("${resend.api-key}")
    private String apiKey;

    @Value("${resend.from-email}")
    private String fromEmail;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    private Resend resend;
    private String internalTemplate;
    private String externalTemplate;

    @PostConstruct
    public void init() throws IOException {
        this.resend = new Resend(apiKey);
        this.internalTemplate = loadTemplate("templates/email/invitation-internal.html");
        this.externalTemplate = loadTemplate("templates/email/invitation-external.html");
    }

    public void sendInvitationEmail(InvitationCreatedEvent event) {
        if (notificationLogRepository.existsByInvitationId(event.invitationId())) {
            log.warn("Email already sent for invitation {}, skipping", event.invitationId());
            return;
        }

        String mapsUrl = "https://www.google.com/maps?q=" + event.latitude() + "," + event.longitude();

        String html = event.external()
                ? buildHtml(externalTemplate, event, mapsUrl)
                : buildHtml(internalTemplate, event, mapsUrl);

        try {
            CreateEmailOptions options = CreateEmailOptions.builder()
                    .from(fromEmail)
                    .to(event.invitedEmail())
                    .subject("You've been invited to " + event.eventTitle())
                    .html(html)
                    .build();

            notificationLogRepository.save(
                    NotificationLog.builder()
                            .invitationId(event.invitationId())
                            .sentAt(LocalDateTime.now())
                            .build());

            resend.emails().send(options);
        } catch (ResendException e) {
            throw new RuntimeException("Failed to send email for invitation " +
                    event.invitationId(), e);
        }
    }

    private String buildHtml(String template, InvitationCreatedEvent event, String mapsUrl) {
        return template
                .replace("{{organizerName}}", event.organizerName())
                .replace("{{eventTitle}}", event.eventTitle())
                .replace("{{eventDate}}", event.eventDate())
                .replace("{{mapsUrl}}", mapsUrl)
                .replace("{{eventLocation}}", event.eventLocation())
                .replace("{{workspaceUrl}}", frontendUrl + "/workspace?tab=invitations")
                .replace("{{inviteUrl}}", frontendUrl + "/invite/" + event.invitationId());
    }

    private String loadTemplate(String path) throws IOException {
        return new ClassPathResource(path)
                .getContentAsString(StandardCharsets.UTF_8);
    }
}