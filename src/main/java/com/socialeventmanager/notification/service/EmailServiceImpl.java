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
import com.socialeventmanager.kafka.event.EventReminderEvent;
import com.socialeventmanager.kafka.event.InvitationCreatedEvent;
import com.socialeventmanager.kafka.event.InvitationRespondedEvent;
import com.socialeventmanager.kafka.event.PasswordResetRequestedEvent;
import com.socialeventmanager.kafka.event.UserRegisteredEvent;
import com.socialeventmanager.shared.exception.EmailDeliveryException;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private static final String EVENT_DATE = "{{eventDate}}";
    private static final String EVENT_LOCATION = "{{eventLocation}}";
    private static final String ORGANIZER_NAME = "{{organizerName}}";
    private static final String MAPS_URL = "{{mapsUrl}}";
    private static final String EVENT_TITLE = "{{eventTitle}}";
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
    private String internalTemplateEs;

    private String externalTemplate;
    private String externalTemplateEs;

    private String welcomeTemplate;
    private String welcomeTemplateEs;

    private String eventCancelledTemplate;
    private String eventCancelledTemplateEs;

    private String invitationRespondedTemplate;
    private String invitationRespondedTemplateEs;

    private String eventReminderTemplate;
    private String eventReminderTemplateEs;

    private String passwordResetTemplate;
    private String passwordResetTemplateEs;

    @PostConstruct
    public void init() throws IOException {
        this.resend = new Resend(apiKey);
        this.internalTemplate = loadTemplate("templates/email/invitation-internal.html");
        this.internalTemplateEs = loadTemplate("templates/email/invitation-internal-es.html");
        this.externalTemplate = loadTemplate("templates/email/invitation-external.html");
        this.externalTemplateEs = loadTemplate("templates/email/invitation-external-es.html");
        this.welcomeTemplate = loadTemplate("templates/email/welcome.html");
        this.welcomeTemplateEs = loadTemplate("templates/email/welcome-es.html");
        this.eventCancelledTemplate = loadTemplate("templates/email/event-cancelled.html");
        this.eventCancelledTemplateEs = loadTemplate("templates/email/event-cancelled-es.html");
        this.invitationRespondedTemplate = loadTemplate("templates/email/invitation-responded.html");
        this.invitationRespondedTemplateEs = loadTemplate("templates/email/invitation-responded-es.html");
        this.eventReminderTemplate = loadTemplate("templates/email/event-reminder.html");
        this.eventReminderTemplateEs = loadTemplate("templates/email/event-reminder-es.html");
        this.passwordResetTemplate = loadTemplate("templates/email/password-reset.html");
        this.passwordResetTemplateEs = loadTemplate("templates/email/password-reset-es.html");
    }

    @Override
    public void sendInvitationEmail(InvitationCreatedEvent event) {
        if (notificationLogService.existsByInvitationId(event.invitationId())) {
            log.warn("Email already sent for invitation {}, skipping", event.invitationId());
            return;
        }

        String mapsUrl = String.format(GOOGLE_MAPS_URL, event.latitude(), event.longitude());

        boolean isEs = "es".equals(event.language());
        String template;
        if (event.external()) {
            template = isEs ? externalTemplateEs : externalTemplate;
        } else {
            template = isEs ? internalTemplateEs : internalTemplate;
        }
        String html = buildHtml(template, event, mapsUrl);
        String subject = isEs
                ? "Fuiste invitado a " + event.eventTitle()
                : "You've been invited to " + event.eventTitle();

        try {
            CreateEmailOptions options = CreateEmailOptions.builder()
                    .from(fromEmail)
                    .to(resolveRecipient(event.invitedEmail()))
                    .subject(subject)
                    .html(html)
                    .build();

            notificationLogService.save(event.invitationId());

            resend.emails().send(options);
        } catch (ResendException e) {
            throw new EmailDeliveryException("Failed to send email for invitation " +
                    event.invitationId(), e);
        }
    }

    @Override
    public void sendWelcomeEmail(UserRegisteredEvent event) {
        boolean isEs = "es".equals(event.language());
        String template = isEs ? welcomeTemplateEs : welcomeTemplate;

        String html = template
                .replace("{{firstName}}", event.firstName())
                .replace("{{dashboardUrl}}", frontendUrl + "/dashboard");

        String subject = isEs
                ? "¡Bienvenido a Social Event Manager!"
                : "Welcome to Social Event Manager!";

        try {
            CreateEmailOptions options = CreateEmailOptions.builder()
                    .from(fromEmail)
                    .to(resolveRecipient(event.email()))
                    .subject(subject)
                    .html(html)
                    .build();
            resend.emails().send(options);
        } catch (ResendException e) {
            throw new EmailDeliveryException("Failed to send welcome email for user " +
                    event.userId(), e);
        }
    }

    @Override
    public void sendEventCancelledEmail(EventCancelledEvent event) {
        String mapsUrl = String.format(GOOGLE_MAPS_URL, event.latitude(), event.longitude());

        boolean isEs = "es".equals(event.language());
        String template = isEs ? eventCancelledTemplateEs : eventCancelledTemplate;
        String html = template
                .replace(ORGANIZER_NAME, event.organizerName())
                .replace(EVENT_TITLE, event.eventTitle())
                .replace(EVENT_DATE, event.eventDate())
                .replace(MAPS_URL, mapsUrl)
                .replace(EVENT_LOCATION, event.eventLocation());

        String subject = isEs
                ? "Evento cancelado: " + event.eventTitle()
                : "Event cancelled: " + event.eventTitle();

        for (String email : event.participantEmails()) {
            try {
                CreateEmailOptions options = CreateEmailOptions.builder()
                        .from(fromEmail)
                        .to(resolveRecipient(email))
                        .subject(subject)
                        .html(html)
                        .build();

                resend.emails().send(options);

            } catch (ResendException e) {
                throw new EmailDeliveryException("Failed to send cancellation email to " +
                        email + " for event " + event.eventId(), e);
            }
        }
    }

    @Override
    public void sendInvitationRespondedEmail(InvitationRespondedEvent event) {
        boolean isEs = "es".equals(event.language());
        boolean accepted = event.status() == InvitationStatus.ACCEPTED;

        String template = isEs ? invitationRespondedTemplateEs : invitationRespondedTemplate;

        String spanishStatus = accepted
                ? "<span style='color:#16a34a;font-weight:600;'>aceptó</span>"
                : "<span style='color:#dc2626;font-weight:600;'>rechazó</span>";

        String englishStatus = accepted
                ? "<span style='color:#16a34a;font-weight:600;'>accepted</span>"
                : "<span style='color:#dc2626;font-weight:600;'>declined</span>";

        String statusText = isEs ? spanishStatus : englishStatus;

        String html = template
                .replace(EVENT_TITLE, event.eventTitle())
                .replace("{{participantName}}", event.participantName())
                .replace("{{statusText}}", statusText)
                .replace("{{eventUrl}}", frontendUrl + "/events/" + event.eventId());

        String subjectPrefix = accepted ? "✅ " : "❌ ";
        String esResponse = accepted ? " aceptó" : " rechazó";
        String enResponse = accepted ? "accepted" : "declined";

        String subject = isEs
                ? subjectPrefix + event.participantName() + esResponse +
                        " tu invitación a " + event.eventTitle()
                : subjectPrefix + event.participantName() +
                        " " + enResponse +
                        " your invitation to " + event.eventTitle();

        try {
            CreateEmailOptions options = CreateEmailOptions.builder()
                    .from(fromEmail)
                    .to(resolveRecipient(event.organizerEmail()))
                    .subject(subject)
                    .html(html)
                    .build();
            resend.emails().send(options);
        } catch (ResendException e) {
            throw new EmailDeliveryException("Failed to send invitation responded email for invitation " +
                    event.invitationId(), e);
        }
    }

    @Override
    public void sendEventReminderEmail(EventReminderEvent event) {
        String mapsUrl = String.format(GOOGLE_MAPS_URL, event.latitude(), event.longitude());

        boolean isEs = "es".equals(event.language());
        String template = isEs ? eventReminderTemplateEs : eventReminderTemplate;
        String html = template
                .replace(EVENT_TITLE, event.eventTitle())
                .replace(EVENT_DATE, event.eventDate())
                .replace(MAPS_URL, mapsUrl)
                .replace(EVENT_LOCATION, event.eventLocation())
                .replace(ORGANIZER_NAME, event.organizerName())
                .replace("{{eventUrl}}", frontendUrl + "/events/" + event.eventId());

        String subject = isEs
                ? "🗓 Recordatorio: " + event.eventTitle() + " es mañana"
                : "🗓 Reminder: " + event.eventTitle() + " is tomorrow";

        for (String email : event.participantEmails()) {
            try {
                CreateEmailOptions options = CreateEmailOptions.builder()
                        .from(fromEmail)
                        .to(resolveRecipient(email))
                        .subject(subject)
                        .html(html)
                        .build();

                resend.emails().send(options);

            } catch (ResendException e) {
                throw new EmailDeliveryException("Failed to send reminder email to " +
                        email + " for event " + event.eventId(), e);
            }
        }

        try {
            CreateEmailOptions options = CreateEmailOptions.builder()
                    .from(fromEmail)
                    .to(resolveRecipient(event.organizerEmail()))
                    .subject("🗓 Reminder: " + event.eventTitle() + " is tomorrow")
                    .html(html)
                    .build();

            resend.emails().send(options);

        } catch (ResendException e) {
            throw new EmailDeliveryException("Failed to send reminder email to organizer for event " +
                    event.eventId(), e);
        }
    }

    @Override
    public void sendPasswordResetEmail(PasswordResetRequestedEvent event) {
        boolean isEs = "es".equals(event.language());
        String template = isEs ? passwordResetTemplateEs : passwordResetTemplate;

        String resetUrl = frontendUrl + "/reset-password?token=" + event.resetToken();

        String html = template
                .replace("{{firstName}}", event.firstName())
                .replace("{{resetUrl}}", resetUrl);

        String subject = isEs
                ? "Restablecé tu contraseña"
                : "Reset your password";

        try {
            CreateEmailOptions options = CreateEmailOptions.builder()
                    .from(fromEmail)
                    .to(resolveRecipient(event.email()))
                    .subject(subject)
                    .html(html)
                    .build();
            resend.emails().send(options);
        } catch (ResendException e) {
            throw new EmailDeliveryException("Failed to send password reset email to " +
                    event.email(), e);
        }
    }

    private String buildHtml(String template, InvitationCreatedEvent event, String mapsUrl) {
        return template
                .replace(ORGANIZER_NAME, event.organizerName())
                .replace(EVENT_TITLE, event.eventTitle())
                .replace(EVENT_DATE, event.eventDate())
                .replace(MAPS_URL, mapsUrl)
                .replace(EVENT_LOCATION, event.eventLocation())
                .replace("{{workspaceUrl}}", frontendUrl + "/workspace?tab=invitations")
                .replace("{{inviteUrl}}", frontendUrl + "/invite/" +
                        (event.external() ? event.externalToken() : event.invitationId()));
    }

    private String loadTemplate(String path) throws IOException {
        return new ClassPathResource(path)
                .getContentAsString(StandardCharsets.UTF_8);
    }

    private String resolveRecipient(String actualEmail) {
        return (overrideTo != null && !overrideTo.isBlank()) ? overrideTo : actualEmail;
    }
}