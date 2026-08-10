package com.socialeventmanager.event.scheduler;

import com.socialeventmanager.event.entity.Event;
import com.socialeventmanager.event.enums.InvitationStatus;
import com.socialeventmanager.event.repository.EventRepository;
import com.socialeventmanager.event.repository.InvitationRepository;
import com.socialeventmanager.kafka.event.EventReminderEvent;
import com.socialeventmanager.kafka.producer.EventProducer;
import com.socialeventmanager.shared.util.Constants;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventReminderScheduler {

    private final EventRepository eventRepository;
    private final InvitationRepository invitationRepository;
    private final EventProducer eventProducer;

    @Scheduled(cron = "0 0 10 * * *") // every day at 10:00 AM
    @Transactional
    public void sendReminders() {
        LocalDateTime from = LocalDateTime.now(Constants.TIMEZONE_ARGENTINA)
                .plusDays(1)
                .toLocalDate()
                .atStartOfDay();
        LocalDateTime to = from.plusDays(1).minusSeconds(1);

        List<Event> events = eventRepository.findEventsForReminder(from, to);

        if (events.isEmpty())
            return;

        for (Event event : events) {
            List<String> participantEmails = invitationRepository
                    .findAllByEventAndStatus(event, InvitationStatus.ACCEPTED)
                    .stream()
                    .map(inv -> inv.getInvitedUser().getEmail())
                    .toList();

            eventProducer.sendEventReminder(new EventReminderEvent(
                    event.getId(),
                    event.getTitle(),
                    event.getLocation(),
                    event.getEventDate().toString(),
                    event.getCreatedBy().getFirstName() + " " +
                            event.getCreatedBy().getLastName(),
                    event.getCreatedBy().getEmail(),
                    event.getLatitude(),
                    event.getLongitude(),
                    participantEmails,
                    event.getLanguage()));

            event.setReminderSent(true);
            eventRepository.save(event);
        }
    }
}