package com.socialeventmanager.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
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
import com.socialeventmanager.event.dto.UpdateInvitationStatusRequestDTO;
import com.socialeventmanager.event.entity.Event;
import com.socialeventmanager.event.entity.EventInvitation;
import com.socialeventmanager.event.enums.EventStatus;
import com.socialeventmanager.event.enums.InvitationStatus;
import com.socialeventmanager.event.repository.EventRepository;
import com.socialeventmanager.event.repository.InvitationRepository;
import com.socialeventmanager.event.service.InvitationServiceImpl;
import com.socialeventmanager.kafka.producer.EventProducer;
import com.socialeventmanager.notification.service.NotificationLogService;
import com.socialeventmanager.shared.dto.ApiResponseDTO;
import com.socialeventmanager.shared.exception.BadRequestException;
import com.socialeventmanager.shared.util.EventValidator;
import com.socialeventmanager.user.entity.User;
import com.socialeventmanager.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("InvitationServiceImpl")
class InvitationServiceImplTest {

    @Mock
    private InvitationRepository invitationRepository;
    @Mock
    private UserRepository userRepository;
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
    private InvitationServiceImpl invitationService;

    private User currentUser;
    private User invitedUser;
    private Event event;
    private UUID eventId;

    @BeforeEach
    void setUp() {
        currentUser = User.builder()
                .firstName("Facundo")
                .lastName("Torres")
                .email("facundo@test.com")
                .password("encodedPassword")
                .hasPassword(true)
                .build();
        currentUser.setId(UUID.randomUUID());

        invitedUser = User.builder()
                .firstName("Juan")
                .lastName("Perez")
                .email("juan@test.com")
                .password("encodedPassword")
                .hasPassword(true)
                .build();
        invitedUser.setId(UUID.randomUUID());

        eventId = UUID.randomUUID();

        event = Event.builder()
                .title("Test Event")
                .description("Test Description")
                .eventDate(LocalDateTime.now().plusDays(1))
                .location("Test Location")
                .locationAddress("Test Address")
                .placeId("test-place-id")
                .latitude(-32.9)
                .longitude(-60.6)
                .createdBy(currentUser)
                .status(EventStatus.ACTIVE)
                .language("en")
                .build();
        event.setId(eventId);
    }

    @Nested
    @DisplayName("inviteExistingUser")
    class InviteExistingUser {

        @Test
        @DisplayName("should invite user successfully")
        void shouldInviteUserSuccessfully() {
            when(invitationRepository.findByEventAndInvitedUser(event, invitedUser))
                    .thenReturn(Optional.empty());
            when(invitationRepository.save(any(EventInvitation.class)))
                    .thenReturn(null);

            ApiResponseDTO<Void> response = invitationService
                    .inviteExistingUser(event, currentUser, invitedUser, "en");

            assertThat(response.isSuccess()).isTrue();
            verify(invitationRepository).save(any(EventInvitation.class));
            verify(eventProducer).sendInvitationCreated(any());
            verify(eventProducer).sendNotification(any());
        }

        @Test
        @DisplayName("should throw when user already invited")
        void shouldThrowWhenUserAlreadyInvited() {
            EventInvitation existingInvitation = EventInvitation.builder()
                    .event(event)
                    .invitedUser(invitedUser)
                    .invitedBy(currentUser)
                    .status(InvitationStatus.PENDING)
                    .build();
            existingInvitation.setId(UUID.randomUUID());

            when(invitationRepository.findByEventAndInvitedUser(event, invitedUser))
                    .thenReturn(Optional.of(existingInvitation));

            assertThatThrownBy(() -> invitationService.inviteExistingUser(event, currentUser, invitedUser, "en"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("userAlreadyInvited");
        }

        @Test
        @DisplayName("should reactivate cancelled invitation")
        void shouldReactivateCancelledInvitation() {
            EventInvitation existingInvitation = EventInvitation.builder()
                    .event(event)
                    .invitedUser(invitedUser)
                    .invitedBy(currentUser)
                    .status(InvitationStatus.CANCELLED)
                    .build();
            existingInvitation.setId(UUID.randomUUID());

            when(invitationRepository.findByEventAndInvitedUser(event, invitedUser))
                    .thenReturn(Optional.of(existingInvitation));
            when(invitationRepository.save(any())).thenReturn(null);

            ApiResponseDTO<Void> response = invitationService
                    .inviteExistingUser(event, currentUser, invitedUser, "en");

            assertThat(response.isSuccess()).isTrue();
            assertThat(existingInvitation.getStatus()).isEqualTo(InvitationStatus.PENDING);
            verify(notificationLogService).deleteByInvitationId(existingInvitation.getId());
        }

        @Test
        @DisplayName("should reactivate rejected invitation")
        void shouldReactivateRejectedInvitation() {
            EventInvitation existingInvitation = EventInvitation.builder()
                    .event(event)
                    .invitedUser(invitedUser)
                    .invitedBy(currentUser)
                    .status(InvitationStatus.REJECTED)
                    .build();
            existingInvitation.setId(UUID.randomUUID());

            when(invitationRepository.findByEventAndInvitedUser(event, invitedUser))
                    .thenReturn(Optional.of(existingInvitation));
            when(invitationRepository.save(any())).thenReturn(null);

            ApiResponseDTO<Void> response = invitationService
                    .inviteExistingUser(event, currentUser, invitedUser, "en");

            assertThat(response.isSuccess()).isTrue();
            assertThat(existingInvitation.getStatus()).isEqualTo(InvitationStatus.PENDING);
        }
    }

    @Nested
    @DisplayName("updateInvitationStatus")
    class UpdateInvitationStatus {

        @Test
        @DisplayName("should accept invitation successfully")
        void shouldAcceptInvitationSuccessfully() {
            EventInvitation invitation = EventInvitation.builder()
                    .event(event)
                    .invitedUser(invitedUser)
                    .invitedBy(currentUser)
                    .status(InvitationStatus.PENDING)
                    .build();
            invitation.setId(UUID.randomUUID());

            UpdateInvitationStatusRequestDTO request = new UpdateInvitationStatusRequestDTO();
            request.setEventId(eventId);
            request.setStatus(InvitationStatus.ACCEPTED);

            when(currentUserService.getCurrentUser()).thenReturn(invitedUser);
            when(invitationRepository.findByEventIdAndInvitedUser(eventId, invitedUser))
                    .thenReturn(Optional.of(invitation));
            when(invitationRepository.save(any())).thenReturn(null);

            ApiResponseDTO<Void> response = invitationService.updateInvitationStatus(request, "en");

            assertThat(response.isSuccess()).isTrue();
            assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.ACCEPTED);
            verify(eventProducer).sendInvitationResponded(any());
            verify(eventProducer).sendNotification(any());
        }

        @Test
        @DisplayName("should throw when status is pending")
        void shouldThrowWhenStatusIsPending() {
            UpdateInvitationStatusRequestDTO request = new UpdateInvitationStatusRequestDTO();
            request.setEventId(eventId);
            request.setStatus(InvitationStatus.PENDING);

            when(currentUserService.getCurrentUser()).thenReturn(invitedUser);

            assertThatThrownBy(() -> invitationService.updateInvitationStatus(request, "en"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("invalidInvitationStatus");
        }

        @Test
        @DisplayName("should throw when invitation not found")
        void shouldThrowWhenInvitationNotFound() {
            UpdateInvitationStatusRequestDTO request = new UpdateInvitationStatusRequestDTO();
            request.setEventId(eventId);
            request.setStatus(InvitationStatus.ACCEPTED);

            when(currentUserService.getCurrentUser()).thenReturn(invitedUser);
            when(invitationRepository.findByEventIdAndInvitedUser(eventId, invitedUser))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> invitationService.updateInvitationStatus(request, "en"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("invitationNotFound");
        }

        @Test
        @DisplayName("should throw when invitation already has this status")
        void shouldThrowWhenInvitationAlreadyHasStatus() {
            EventInvitation invitation = EventInvitation.builder()
                    .event(event)
                    .invitedUser(invitedUser)
                    .invitedBy(currentUser)
                    .status(InvitationStatus.ACCEPTED)
                    .build();
            invitation.setId(UUID.randomUUID());

            UpdateInvitationStatusRequestDTO request = new UpdateInvitationStatusRequestDTO();
            request.setEventId(eventId);
            request.setStatus(InvitationStatus.ACCEPTED);

            when(currentUserService.getCurrentUser()).thenReturn(invitedUser);
            when(invitationRepository.findByEventIdAndInvitedUser(eventId, invitedUser))
                    .thenReturn(Optional.of(invitation));

            assertThatThrownBy(() -> invitationService.updateInvitationStatus(request, "en"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("invitationAlreadyHasStatus");
        }

        @Test
        @DisplayName("should throw when invitation is cancelled")
        void shouldThrowWhenInvitationIsCancelled() {
            EventInvitation invitation = EventInvitation.builder()
                    .event(event)
                    .invitedUser(invitedUser)
                    .invitedBy(currentUser)
                    .status(InvitationStatus.CANCELLED)
                    .build();
            invitation.setId(UUID.randomUUID());

            UpdateInvitationStatusRequestDTO request = new UpdateInvitationStatusRequestDTO();
            request.setEventId(eventId);
            request.setStatus(InvitationStatus.ACCEPTED);

            when(currentUserService.getCurrentUser()).thenReturn(invitedUser);
            when(invitationRepository.findByEventIdAndInvitedUser(eventId, invitedUser))
                    .thenReturn(Optional.of(invitation));

            assertThatThrownBy(() -> invitationService.updateInvitationStatus(request, "en"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("invitationAlreadyCancelled");
        }
    }

    @Nested
    @DisplayName("getAcceptedParticipantEmails")
    class GetAcceptedParticipantEmails {

        @Test
        @DisplayName("should return emails of accepted participants")
        void shouldReturnEmailsOfAcceptedParticipants() {
            EventInvitation invitation = EventInvitation.builder()
                    .event(event)
                    .invitedUser(invitedUser)
                    .invitedBy(currentUser)
                    .status(InvitationStatus.ACCEPTED)
                    .build();

            when(invitationRepository.findAllByEventAndStatus(event, InvitationStatus.ACCEPTED))
                    .thenReturn(List.of(invitation));

            List<String> emails = invitationService.getAcceptedParticipantEmails(event);

            assertThat(emails).containsExactly("juan@test.com");
        }

        @Test
        @DisplayName("should return empty list when no accepted participants")
        void shouldReturnEmptyListWhenNoAcceptedParticipants() {
            when(invitationRepository.findAllByEventAndStatus(event, InvitationStatus.ACCEPTED))
                    .thenReturn(List.of());

            List<String> emails = invitationService.getAcceptedParticipantEmails(event);

            assertThat(emails).isEmpty();
        }
    }

    @Nested
    @DisplayName("cancelInvitationsForEvent")
    class CancelInvitationsForEvent {

        @Test
        @DisplayName("should cancel all invitations for event")
        void shouldCancelAllInvitationsForEvent() {
            EventInvitation invitation1 = EventInvitation.builder()
                    .event(event)
                    .invitedUser(invitedUser)
                    .invitedBy(currentUser)
                    .status(InvitationStatus.ACCEPTED)
                    .build();

            EventInvitation invitation2 = EventInvitation.builder()
                    .event(event)
                    .invitedUser(currentUser)
                    .invitedBy(currentUser)
                    .status(InvitationStatus.PENDING)
                    .build();

            when(invitationRepository.findAllByEvent(event))
                    .thenReturn(List.of(invitation1, invitation2));

            invitationService.cancelInvitationsForEvent(event);

            assertThat(invitation1.getStatus()).isEqualTo(InvitationStatus.CANCELLED);
            assertThat(invitation2.getStatus()).isEqualTo(InvitationStatus.CANCELLED);
            verify(invitationRepository).saveAll(anyList());
        }
    }
}