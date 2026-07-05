package com.socialeventmanager.notification.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.socialeventmanager.event.enums.InvitationStatus;
import com.socialeventmanager.kafka.event.EventCancelledEvent;
import com.socialeventmanager.kafka.event.InvitationCreatedEvent;
import com.socialeventmanager.kafka.event.InvitationRespondedEvent;
import com.socialeventmanager.kafka.event.UserRegisteredEvent;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private static final String GOOGLE_MAPS_URL = "https://www.google.com/maps?q=%s,%s";
    private final NotificationLogService notificationLogService;

    @Value("${resend.api-key}")
    private String apiKey;

    @Value("${resend.from-email}")
    private String fromEmail;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${resend.override-to:}")
    private String overrideTo;

    private Resend resend;
    private String internalTemplate;
    private String externalTemplate;
    private String welcomeTemplate;
    private String eventCancelledTemplate;
    private String invitationRespondedTemplate;

    @PostConstruct
    public void init() throws IOException {
        this.resend = new Resend(apiKey);
        this.internalTemplate = loadTemplate("templates/email/invitation-internal.html");
        this.externalTemplate = loadTemplate("templates/email/invitation-external.html");
        this.welcomeTemplate = loadTemplate("templates/email/welcome.html");
        this.eventCancelledTemplate = loadTemplate("templates/email/event-cancelled.html");
        this.invitationRespondedTemplate = loadTemplate("templates/email/invitation-responded.html");
    }

    @Override
    public void sendInvitationEmail(InvitationCreatedEvent event) {
        if (notificationLogService.existsByInvitationId(event.invitationId())) {
            log.warn("Email already sent for invitation {}, skipping", event.invitationId());
            return;
        }

        String mapsUrl = String.format(GOOGLE_MAPS_URL, event.latitude(), event.longitude());

        String html = event.external()
                ? buildHtml(externalTemplate, event, mapsUrl)
                : buildHtml(internalTemplate, event, mapsUrl);

        try {
            CreateEmailOptions options = CreateEmailOptions.builder()
                    .from(fromEmail)
                    .to(resolveRecipient(event.invitedEmail()))
                    .subject("You've been invited to " + event.eventTitle())
                    .html(html)
                    .build();

            notificationLogService.save(event.invitationId());

            resend.emails().send(options);
        } catch (ResendException e) {
            throw new RuntimeException("Failed to send email for invitation " +
                    event.invitationId(), e);
        }
    }

    @Override
    public void sendWelcomeEmail(UserRegisteredEvent event) {
        String html = welcomeTemplate
                .replace("{{firstName}}", event.firstName())
                .replace("{{dashboardUrl}}", frontendUrl + "/dashboard");

        try {
            CreateEmailOptions options = CreateEmailOptions.builder()
                    .from(fromEmail)
                    .to(resolveRecipient(event.email()))
                    .subject("Welcome to Social Event Manager!")
                    .html(html)
                    .build();

            resend.emails().send(options);

        } catch (ResendException e) {
            throw new RuntimeException("Failed to send welcome email for user " +
                    event.userId(), e);
        }
    }

    @Override
    public void sendEventCancelledEmail(EventCancelledEvent event) {
        String mapsUrl = String.format(GOOGLE_MAPS_URL, event.latitude(), event.longitude());

        String html = eventCancelledTemplate
                .replace("{{organizerName}}", event.organizerName())
                .replace("{{eventTitle}}", event.eventTitle())
                .replace("{{eventDate}}", event.eventDate())
                .replace("{{mapsUrl}}", mapsUrl)
                .replace("{{eventLocation}}", event.eventLocation());

        for (String email : event.participantEmails()) {
            try {
                CreateEmailOptions options = CreateEmailOptions.builder()
                        .from(fromEmail)
                        .to(resolveRecipient(email))
                        .subject("Event cancelled: " + event.eventTitle())
                        .html(html)
                        .build();

                resend.emails().send(options);

            } catch (ResendException e) {
                throw new RuntimeException("Failed to send cancellation email to " +
                        email + " for event " + event.eventId(), e);
            }
        }
    }

    @Override
    public void sendInvitationRespondedEmail(InvitationRespondedEvent event) {
        boolean accepted = event.status() == InvitationStatus.ACCEPTED;

        String statusText = accepted
                ? "<span style='color:#16a34a;font-weight:600;'>accepted</span>"
                : "<span style='color:#dc2626;font-weight:600;'>declined</span>";

        String html = invitationRespondedTemplate
                .replace("{{eventTitle}}", event.eventTitle())
                .replace("{{participantName}}", event.participantName())
                .replace("{{statusLabel}}", accepted ? "accepted" : "declined")
                .replace("{{statusText}}", statusText)
                .replace("{{eventUrl}}", frontendUrl + "/events/" + event.eventId());

        try {
            CreateEmailOptions options = CreateEmailOptions.builder()
                    .from(fromEmail)
                    .to(resolveRecipient(event.organizerEmail()))
                    .subject((accepted ? "✅ " : "❌ ") + event.participantName() +
                            " " + (accepted ? "accepted" : "declined") +
                            " your invitation to " + event.eventTitle())
                    .html(html)
                    .build();

            resend.emails().send(options);

        } catch (ResendException e) {
            throw new RuntimeException("Failed to send invitation responded email for invitation " +
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

    private String resolveRecipient(String actualEmail) {
        return (overrideTo != null && !overrideTo.isBlank()) ? overrideTo : actualEmail;
    }
}