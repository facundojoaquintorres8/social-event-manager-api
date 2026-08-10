package com.socialeventmanager.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.socialeventmanager.auth.service.CurrentUserService;
import com.socialeventmanager.event.entity.Event;
import com.socialeventmanager.event.entity.ExternalInvitation;
import com.socialeventmanager.event.enums.EventStatus;
import com.socialeventmanager.event.enums.ExternalInvitationStatus;
import com.socialeventmanager.event.repository.EventRepository;
import com.socialeventmanager.event.repository.ExternalInvitationRepository;
import com.socialeventmanager.event.service.ExternalInvitationServiceImpl;
import com.socialeventmanager.event.service.InvitationService;
import com.socialeventmanager.kafka.producer.EventProducer;
import com.socialeventmanager.notification.service.NotificationLogService;
import com.socialeventmanager.shared.dto.ApiResponseDTO;
import com.socialeventmanager.shared.exception.BadRequestException;
import com.socialeventmanager.shared.util.Constants;
import com.socialeventmanager.shared.util.EventValidator;
import com.socialeventmanager.user.entity.User;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExternalInvitationServiceImpl")
class ExternalInvitationServiceImplTest {

    @Mock
    private ExternalInvitationRepository externalInvitationRepository;
    @Mock
    private InvitationService invitationService;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private EventValidator eventValidator;
    @Mock
    private EventProducer eventProducer;
    @Mock
    private NotificationLogService notificationLogService;

    @InjectMocks
    private ExternalInvitationServiceImpl externalInvitationService;

    private User organizer;
    private Event event;
    private UUID eventId;

    @BeforeEach
    void setUp() {
        organizer = User.builder()
                .firstName("Facundo")
                .lastName("Torres")
                .email("facundo@test.com")
                .hasPassword(true)
                .build();
        organizer.setId(UUID.randomUUID());

        eventId = UUID.randomUUID();

        event = Event.builder()
                .title("Test Event")
                .eventDate(LocalDateTime.now(Constants.TIMEZONE_ARGENTINA).plusDays(1))
                .location("Test Location")
                .locationAddress("Test Address")
                .placeId("test-place-id")
                .latitude(-32.9)
                .longitude(-60.6)
                .createdBy(organizer)
                .status(EventStatus.ACTIVE)
                .language("en")
                .build();
        event.setId(eventId);
    }

    @Nested
    @DisplayName("inviteExternalUser")
    class InviteExternalUser {

        @Test
        @DisplayName("should invite external user successfully")
        void shouldInviteExternalUserSuccessfully() {
            when(externalInvitationRepository.findByEventAndInvitedEmail(event, "external@test.com"))
                    .thenReturn(Optional.empty());
            when(externalInvitationRepository.save(any())).thenReturn(null);

            ApiResponseDTO<Void> response = externalInvitationService
                    .inviteExternalUser(event, organizer, "external@test.com", "en");

            assertThat(response.isSuccess()).isTrue();
            verify(externalInvitationRepository).save(any(ExternalInvitation.class));
            verify(eventProducer).sendInvitationCreated(any());
        }

        @Test
        @DisplayName("should throw when user already invited")
        void shouldThrowWhenUserAlreadyInvited() {
            ExternalInvitation existingInvitation = ExternalInvitation.builder()
                    .event(event)
                    .invitedBy(organizer)
                    .invitedEmail("external@test.com")
                    .token(UUID.randomUUID().toString())
                    .status(ExternalInvitationStatus.PENDING)
                    .build();
            existingInvitation.setId(UUID.randomUUID());

            when(externalInvitationRepository.findByEventAndInvitedEmail(event, "external@test.com"))
                    .thenReturn(Optional.of(existingInvitation));

            assertThatThrownBy(() -> externalInvitationService
                    .inviteExternalUser(event, organizer, "external@test.com", "en"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("userAlreadyInvited");
        }

        @Test
        @DisplayName("should reactivate cancelled external invitation")
        void shouldReactivateCancelledExternalInvitation() {
            ExternalInvitation existingInvitation = ExternalInvitation.builder()
                    .event(event)
                    .invitedBy(organizer)
                    .invitedEmail("external@test.com")
                    .token(UUID.randomUUID().toString())
                    .status(ExternalInvitationStatus.CANCELLED)
                    .build();
            existingInvitation.setId(UUID.randomUUID());

            when(externalInvitationRepository.findByEventAndInvitedEmail(event, "external@test.com"))
                    .thenReturn(Optional.of(existingInvitation));
            when(externalInvitationRepository.save(any())).thenReturn(null);

            ApiResponseDTO<Void> response = externalInvitationService
                    .inviteExternalUser(event, organizer, "external@test.com", "en");

            assertThat(response.isSuccess()).isTrue();
            assertThat(existingInvitation.getStatus()).isEqualTo(ExternalInvitationStatus.PENDING);
            verify(notificationLogService).deleteByInvitationId(existingInvitation.getId());
            verify(eventProducer).sendInvitationCreated(any());
        }
    }

    @Nested
    @DisplayName("getInvitationPreview")
    class GetInvitationPreview {

        @Test
        @DisplayName("should return invitation preview successfully")
        void shouldReturnInvitationPreviewSuccessfully() {
            ExternalInvitation invitation = ExternalInvitation.builder()
                    .event(event)
                    .invitedBy(organizer)
                    .invitedEmail("external@test.com")
                    .token("valid-token")
                    .status(ExternalInvitationStatus.PENDING)
                    .build();
            invitation.setId(UUID.randomUUID());

            when(externalInvitationRepository.findByToken("valid-token"))
                    .thenReturn(Optional.of(invitation));

            var response = externalInvitationService.getInvitationPreview("valid-token");

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData().getTitle()).isEqualTo("Test Event");
            assertThat(response.getData().isAlreadyClaimed()).isFalse();
        }

        @Test
        @DisplayName("should throw when token not found")
        void shouldThrowWhenTokenNotFound() {
            when(externalInvitationRepository.findByToken("invalid-token"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> externalInvitationService.getInvitationPreview("invalid-token"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("invitationNotFound");
        }

        @Test
        @DisplayName("should throw when invitation is cancelled")
        void shouldThrowWhenInvitationIsCancelled() {
            ExternalInvitation invitation = ExternalInvitation.builder()
                    .event(event)
                    .invitedBy(organizer)
                    .invitedEmail("external@test.com")
                    .token("cancelled-token")
                    .status(ExternalInvitationStatus.CANCELLED)
                    .build();

            when(externalInvitationRepository.findByToken("cancelled-token"))
                    .thenReturn(Optional.of(invitation));

            assertThatThrownBy(() -> externalInvitationService.getInvitationPreview("cancelled-token"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("invitationCancelled");
        }

        @Test
        @DisplayName("should throw when event date is in the past")
        void shouldThrowWhenEventDateIsInThePast() {
            event.setEventDate(LocalDateTime.now(Constants.TIMEZONE_ARGENTINA).minusDays(1));

            ExternalInvitation invitation = ExternalInvitation.builder()
                    .event(event)
                    .invitedBy(organizer)
                    .invitedEmail("external@test.com")
                    .token("expired-token")
                    .status(ExternalInvitationStatus.PENDING)
                    .build();

            when(externalInvitationRepository.findByToken("expired-token"))
                    .thenReturn(Optional.of(invitation));

            assertThatThrownBy(() -> externalInvitationService.getInvitationPreview("expired-token"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("invitationExpired");
        }

        @Test
        @DisplayName("should throw when event is cancelled")
        void shouldThrowWhenEventIsCancelled() {
            event.setStatus(EventStatus.CANCELLED);

            ExternalInvitation invitation = ExternalInvitation.builder()
                    .event(event)
                    .invitedBy(organizer)
                    .invitedEmail("external@test.com")
                    .token("event-cancelled-token")
                    .status(ExternalInvitationStatus.PENDING)
                    .build();

            when(externalInvitationRepository.findByToken("event-cancelled-token"))
                    .thenReturn(Optional.of(invitation));

            assertThatThrownBy(() -> externalInvitationService.getInvitationPreview("event-cancelled-token"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("eventCancelled");
        }
    }

    @Nested
    @DisplayName("claimExternalInvitations")
    class ClaimExternalInvitations {

        @Test
        @DisplayName("should claim pending invitations for user")
        void shouldClaimPendingInvitationsForUser() {
            User newUser = User.builder()
                    .firstName("Maria")
                    .lastName("Lopez")
                    .email("external@test.com")
                    .build();
            newUser.setId(UUID.randomUUID());

            ExternalInvitation invitation = ExternalInvitation.builder()
                    .event(event)
                    .invitedBy(organizer)
                    .invitedEmail("external@test.com")
                    .token(UUID.randomUUID().toString())
                    .status(ExternalInvitationStatus.PENDING)
                    .build();
            invitation.setId(UUID.randomUUID());

            when(externalInvitationRepository.findAllByInvitedEmailAndStatus(
                    "external@test.com", ExternalInvitationStatus.PENDING))
                    .thenReturn(List.of(invitation));
            when(invitationService.inviteExistingUser(any(), any(), any(), any()))
                    .thenReturn(new ApiResponseDTO<>(true, "User invited successfully", null));

            externalInvitationService.claimExternalInvitations(newUser, "en");

            assertThat(invitation.getStatus()).isEqualTo(ExternalInvitationStatus.CLAIMED);
            verify(invitationService).inviteExistingUser(event, organizer, newUser, "en");
            verify(externalInvitationRepository).save(invitation);
        }

        @Test
        @DisplayName("should skip cancelled event invitations")
        void shouldSkipCancelledEventInvitations() {
            User newUser = User.builder()
                    .firstName("Maria")
                    .lastName("Lopez")
                    .email("external@test.com")
                    .build();
            newUser.setId(UUID.randomUUID());

            event.setStatus(EventStatus.CANCELLED);

            ExternalInvitation invitation = ExternalInvitation.builder()
                    .event(event)
                    .invitedBy(organizer)
                    .invitedEmail("external@test.com")
                    .token(UUID.randomUUID().toString())
                    .status(ExternalInvitationStatus.PENDING)
                    .build();

            when(externalInvitationRepository.findAllByInvitedEmailAndStatus(
                    "external@test.com", ExternalInvitationStatus.PENDING))
                    .thenReturn(List.of(invitation));

            externalInvitationService.claimExternalInvitations(newUser, "en");

            verify(invitationService, never()).inviteExistingUser(any(), any(), any(), any());
            assertThat(invitation.getStatus()).isEqualTo(ExternalInvitationStatus.PENDING);
        }

        @Test
        @DisplayName("should skip past event invitations")
        void shouldSkipPastEventInvitations() {
            User newUser = User.builder()
                    .firstName("Maria")
                    .lastName("Lopez")
                    .email("external@test.com")
                    .build();
            newUser.setId(UUID.randomUUID());

            event.setEventDate(LocalDateTime.now(Constants.TIMEZONE_ARGENTINA).minusDays(1));

            ExternalInvitation invitation = ExternalInvitation.builder()
                    .event(event)
                    .invitedBy(organizer)
                    .invitedEmail("external@test.com")
                    .token(UUID.randomUUID().toString())
                    .status(ExternalInvitationStatus.PENDING)
                    .build();

            when(externalInvitationRepository.findAllByInvitedEmailAndStatus(
                    "external@test.com", ExternalInvitationStatus.PENDING))
                    .thenReturn(List.of(invitation));

            externalInvitationService.claimExternalInvitations(newUser, "en");

            verify(invitationService, never()).inviteExistingUser(any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("cancelExternalInvitationsForEvent")
    class CancelExternalInvitationsForEvent {

        @Test
        @DisplayName("should cancel all external invitations for event")
        void shouldCancelAllExternalInvitationsForEvent() {
            ExternalInvitation invitation1 = ExternalInvitation.builder()
                    .event(event)
                    .invitedBy(organizer)
                    .invitedEmail("one@test.com")
                    .token(UUID.randomUUID().toString())
                    .status(ExternalInvitationStatus.PENDING)
                    .build();

            ExternalInvitation invitation2 = ExternalInvitation.builder()
                    .event(event)
                    .invitedBy(organizer)
                    .invitedEmail("two@test.com")
                    .token(UUID.randomUUID().toString())
                    .status(ExternalInvitationStatus.PENDING)
                    .build();

            when(externalInvitationRepository.findAllByEvent(event))
                    .thenReturn(List.of(invitation1, invitation2));

            externalInvitationService.cancelExternalInvitationsForEvent(event);

            assertThat(invitation1.getStatus()).isEqualTo(ExternalInvitationStatus.CANCELLED);
            assertThat(invitation2.getStatus()).isEqualTo(ExternalInvitationStatus.CANCELLED);
            verify(externalInvitationRepository).saveAll(anyList());
        }
    }
}