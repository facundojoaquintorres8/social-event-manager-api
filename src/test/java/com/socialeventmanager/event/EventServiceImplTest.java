package com.socialeventmanager.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import com.socialeventmanager.event.dto.CreateEventRequestDTO;
import com.socialeventmanager.event.dto.DashboardResponseDTO;
import com.socialeventmanager.event.dto.EventResponseDTO;
import com.socialeventmanager.event.dto.InviteUserRequestDTO;
import com.socialeventmanager.event.entity.Event;
import com.socialeventmanager.event.enums.EventStatus;
import com.socialeventmanager.event.repository.EventRepository;
import com.socialeventmanager.event.service.ContributionService;
import com.socialeventmanager.event.service.EventServiceImpl;
import com.socialeventmanager.event.service.ExternalInvitationService;
import com.socialeventmanager.event.service.InvitationService;
import com.socialeventmanager.kafka.producer.EventProducer;
import com.socialeventmanager.shared.dto.ApiResponseDTO;
import com.socialeventmanager.shared.exception.BadRequestException;
import com.socialeventmanager.shared.util.Constants;
import com.socialeventmanager.shared.util.EventValidator;
import com.socialeventmanager.user.entity.User;
import com.socialeventmanager.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventServiceImpl")
class EventServiceImplTest {

    @Mock
    private EventRepository eventRepository;
    @Mock
    private InvitationService invitationService;
    @Mock
    private ExternalInvitationService externalInvitationService;
    @Mock
    private ContributionService contributionService;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private EventValidator eventValidator;
    @Mock
    private EventProducer eventProducer;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private EventServiceImpl eventService;

    private User currentUser;
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

        eventId = UUID.randomUUID();

        event = Event.builder()
                .title("Test Event")
                .description("Test Description")
                .eventDate(LocalDateTime.now(Constants.TIMEZONE_ARGENTINA).plusDays(1))
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
    @DisplayName("createEvent")
    class CreateEvent {

        @Test
        @DisplayName("should create event successfully")
        void shouldCreateEventSuccessfully() {
            CreateEventRequestDTO request = new CreateEventRequestDTO();
            request.setTitle("Test Event");
            request.setDescription("Test Description");
            request.setEventDate(LocalDateTime.now(Constants.TIMEZONE_ARGENTINA).plusDays(1));
            request.setLocation("Test Location");
            request.setLocationAddress("Test Address");
            request.setPlaceId("test-place-id");
            request.setLatitude(-32.9);
            request.setLongitude(-60.6);
            request.setLanguage("en");

            when(currentUserService.getCurrentUser()).thenReturn(currentUser);
            when(eventRepository.save(any(Event.class))).thenReturn(event);

            ApiResponseDTO<EventResponseDTO> response = eventService.createEvent(request);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData().getTitle()).isEqualTo("Test Event");
            verify(eventRepository).save(any(Event.class));
        }
    }

    @Nested
    @DisplayName("updateEvent")
    class UpdateEvent {

        @Test
        @DisplayName("should update event successfully")
        void shouldUpdateEventSuccessfully() {
            CreateEventRequestDTO request = new CreateEventRequestDTO();
            request.setTitle("Updated Title");
            request.setDescription("Updated Description");
            request.setEventDate(LocalDateTime.now(Constants.TIMEZONE_ARGENTINA).plusDays(2));
            request.setLocation("Updated Location");
            request.setLocationAddress("Updated Address");
            request.setPlaceId("updated-place-id");
            request.setLatitude(-33.0);
            request.setLongitude(-61.0);

            when(eventRepository.findByIdAndCreatedBy(eventId, currentUser))
                    .thenReturn(Optional.of(event));
            when(currentUserService.getCurrentUser()).thenReturn(currentUser);
            when(eventRepository.save(any(Event.class))).thenReturn(event);
            when(invitationService.getAcceptedParticipantIds(event)).thenReturn(List.of());

            ApiResponseDTO<EventResponseDTO> response = eventService.updateEvent(eventId, request);

            assertThat(response.isSuccess()).isTrue();
            assertThat(event.getTitle()).isEqualTo("Updated Title");
            verify(eventRepository).save(event);
        }

        @Test
        @DisplayName("should send notification when participants exist")
        void shouldSendNotificationWhenParticipantsExist() {
            CreateEventRequestDTO request = new CreateEventRequestDTO();
            request.setTitle("Updated Title");
            request.setDescription("Updated Description");
            request.setEventDate(LocalDateTime.now(Constants.TIMEZONE_ARGENTINA).plusDays(2));
            request.setLocation("Updated Location");
            request.setLocationAddress("Updated Address");
            request.setPlaceId("updated-place-id");
            request.setLatitude(-33.0);
            request.setLongitude(-61.0);

            UUID participantId = UUID.randomUUID();
            when(eventRepository.findByIdAndCreatedBy(eventId, currentUser))
                    .thenReturn(Optional.of(event));
            when(currentUserService.getCurrentUser()).thenReturn(currentUser);
            when(eventRepository.save(any())).thenReturn(event);
            when(invitationService.getAcceptedParticipantIds(event))
                    .thenReturn(List.of(participantId));

            eventService.updateEvent(eventId, request);

            verify(eventProducer).sendNotification(any());
        }

        @Test
        @DisplayName("should throw when event not found")
        void shouldThrowWhenEventNotFound() {
            when(currentUserService.getCurrentUser()).thenReturn(currentUser);
            when(eventRepository.findByIdAndCreatedBy(eventId, currentUser))
                    .thenReturn(Optional.empty());

            CreateEventRequestDTO request = new CreateEventRequestDTO();

            assertThatThrownBy(() -> eventService.updateEvent(eventId, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("eventNotFound");
        }
    }

    @Nested
    @DisplayName("deleteEvent")
    class DeleteEvent {

        @Test
        @DisplayName("should cancel event successfully")
        void shouldCancelEventSuccessfully() {
            when(currentUserService.getCurrentUser()).thenReturn(currentUser);
            when(eventRepository.findByIdAndCreatedBy(eventId, currentUser))
                    .thenReturn(Optional.of(event));
            when(eventRepository.save(any())).thenReturn(event);
            when(invitationService.getAcceptedParticipantEmails(event)).thenReturn(List.of());
            when(invitationService.getAcceptedParticipantIds(event)).thenReturn(List.of());

            ApiResponseDTO<Void> response = eventService.deleteEvent(eventId, "en");

            assertThat(response.isSuccess()).isTrue();
            assertThat(event.getStatus()).isEqualTo(EventStatus.CANCELLED);
            verify(invitationService).cancelInvitationsForEvent(event);
            verify(externalInvitationService).cancelExternalInvitationsForEvent(event);
        }

        @Test
        @DisplayName("should throw when event already cancelled")
        void shouldThrowWhenEventAlreadyCancelled() {
            event.setStatus(EventStatus.CANCELLED);
            when(currentUserService.getCurrentUser()).thenReturn(currentUser);
            when(eventRepository.findByIdAndCreatedBy(eventId, currentUser))
                    .thenReturn(Optional.of(event));

            assertThatThrownBy(() -> eventService.deleteEvent(eventId, "en"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("eventAlreadyCancelled");
        }

        @Test
        @DisplayName("should send email when participants exist")
        void shouldSendEmailWhenParticipantsExist() {
            when(currentUserService.getCurrentUser()).thenReturn(currentUser);
            when(eventRepository.findByIdAndCreatedBy(eventId, currentUser))
                    .thenReturn(Optional.of(event));
            when(eventRepository.save(any())).thenReturn(event);
            when(invitationService.getAcceptedParticipantEmails(event))
                    .thenReturn(List.of("participant@test.com"));
            when(invitationService.getAcceptedParticipantIds(event)).thenReturn(List.of());

            eventService.deleteEvent(eventId, "en");

            verify(eventProducer).sendEventCancelled(any());
        }
    }

    @Nested
    @DisplayName("inviteUser")
    class InviteUser {

        @Test
        @DisplayName("should invite existing user successfully")
        void shouldInviteExistingUserSuccessfully() {
            User invitedUser = User.builder()
                    .email("invited@test.com")
                    .firstName("Juan")
                    .lastName("Perez")
                    .build();
            invitedUser.setId(UUID.randomUUID());

            InviteUserRequestDTO request = new InviteUserRequestDTO();
            request.setEmail("invited@test.com");

            when(currentUserService.getCurrentUser()).thenReturn(currentUser);
            when(eventRepository.findByIdAndCreatedBy(eventId, currentUser))
                    .thenReturn(Optional.of(event));
            when(userRepository.findByEmail("invited@test.com"))
                    .thenReturn(Optional.of(invitedUser));
            when(invitationService.inviteExistingUser(event, currentUser, invitedUser, "en"))
                    .thenReturn(new ApiResponseDTO<>(true, "User invited successfully", null));

            ApiResponseDTO<Void> response = eventService.inviteUser(eventId, request, "en");

            assertThat(response.isSuccess()).isTrue();
            verify(invitationService).inviteExistingUser(event, currentUser, invitedUser, "en");
        }

        @Test
        @DisplayName("should throw when inviting yourself")
        void shouldThrowWhenInvitingYourself() {
            InviteUserRequestDTO request = new InviteUserRequestDTO();
            request.setEmail("facundo@test.com");

            when(currentUserService.getCurrentUser()).thenReturn(currentUser);
            when(eventRepository.findByIdAndCreatedBy(eventId, currentUser))
                    .thenReturn(Optional.of(event));
            when(userRepository.findByEmail("facundo@test.com"))
                    .thenReturn(Optional.of(currentUser));

            assertThatThrownBy(() -> eventService.inviteUser(eventId, request, "en"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("cannotInviteYourself");
        }

        @Test
        @DisplayName("should invite external user when email not registered")
        void shouldInviteExternalUserWhenEmailNotRegistered() {
            InviteUserRequestDTO request = new InviteUserRequestDTO();
            request.setEmail("external@test.com");

            when(currentUserService.getCurrentUser()).thenReturn(currentUser);
            when(eventRepository.findByIdAndCreatedBy(eventId, currentUser))
                    .thenReturn(Optional.of(event));
            when(userRepository.findByEmail("external@test.com"))
                    .thenReturn(Optional.empty());
            when(externalInvitationService.inviteExternalUser(event, currentUser, "external@test.com", "en"))
                    .thenReturn(new ApiResponseDTO<>(true, "Invitation sent successfully", null));

            ApiResponseDTO<Void> response = eventService.inviteUser(eventId, request, "en");

            assertThat(response.isSuccess()).isTrue();
            verify(externalInvitationService).inviteExternalUser(event, currentUser, "external@test.com", "en");
        }
    }

    @Nested
    @DisplayName("getDashboard")
    class GetDashboard {

        @Test
        @DisplayName("should return dashboard data")
        void shouldReturnDashboardData() {
            when(currentUserService.getCurrentUser()).thenReturn(currentUser);
            when(eventRepository.countByCreatedById(currentUser.getId())).thenReturn(5L);
            when(eventRepository.countByCreatedByIdAndStatus(currentUser.getId(), EventStatus.ACTIVE))
                    .thenReturn(3L);
            when(eventRepository.countByCreatedByIdAndStatus(currentUser.getId(), EventStatus.CANCELLED))
                    .thenReturn(2L);
            when(eventRepository.countByCreatedByIdAndEventDateAfter(eq(currentUser.getId()), any()))
                    .thenReturn(1L);
            when(eventRepository.findTop5ByCreatedByIdOrderByCreatedAtDesc(currentUser.getId()))
                    .thenReturn(List.of(event));
            when(invitationService.getRecentInvitations(currentUser)).thenReturn(List.of());

            ApiResponseDTO<DashboardResponseDTO> response = eventService.getDashboard();

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData().totalEvents()).isEqualTo(5L);
            assertThat(response.getData().activeEvents()).isEqualTo(3L);
            assertThat(response.getData().cancelledEvents()).isEqualTo(2L);
        }
    }
}