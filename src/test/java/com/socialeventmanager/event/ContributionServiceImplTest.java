package com.socialeventmanager.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
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
import com.socialeventmanager.event.dto.CreateContributionRequestDTO;
import com.socialeventmanager.event.dto.UpdateContributionStatusRequestDTO;
import com.socialeventmanager.event.entity.Contribution;
import com.socialeventmanager.event.entity.Event;
import com.socialeventmanager.event.entity.EventInvitation;
import com.socialeventmanager.event.enums.EventStatus;
import com.socialeventmanager.event.enums.InvitationStatus;
import com.socialeventmanager.event.repository.ContributionRepository;
import com.socialeventmanager.event.repository.EventRepository;
import com.socialeventmanager.event.repository.InvitationRepository;
import com.socialeventmanager.event.service.ContributionServiceImpl;
import com.socialeventmanager.event.service.InvitationService;
import com.socialeventmanager.kafka.producer.EventProducer;
import com.socialeventmanager.shared.dto.ApiResponseDTO;
import com.socialeventmanager.shared.exception.BadRequestException;
import com.socialeventmanager.shared.util.Constants;
import com.socialeventmanager.shared.util.EventValidator;
import com.socialeventmanager.user.entity.User;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContributionServiceImpl")
class ContributionServiceImplTest {

    @Mock
    private ContributionRepository contributionRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private InvitationRepository invitationRepository;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private EventValidator eventValidator;
    @Mock
    private EventProducer eventProducer;
    @Mock
    private InvitationService invitationService;

    @InjectMocks
    private ContributionServiceImpl contributionService;

    private User organizer;
    private User participant;
    private Event event;
    private Contribution contribution;
    private UUID eventId;
    private UUID contributionId;

    @BeforeEach
    void setUp() {
        organizer = User.builder()
                .firstName("Facundo")
                .lastName("Torres")
                .email("facundo@test.com")
                .password("encodedPassword")
                .hasPassword(true)
                .build();
        organizer.setId(UUID.randomUUID());

        participant = User.builder()
                .firstName("Juan")
                .lastName("Perez")
                .email("juan@test.com")
                .password("encodedPassword")
                .hasPassword(true)
                .build();
        participant.setId(UUID.randomUUID());

        eventId = UUID.randomUUID();
        contributionId = UUID.randomUUID();

        event = Event.builder()
                .title("Test Event")
                .description("Test Description")
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

        contribution = Contribution.builder()
                .event(event)
                .createdBy(participant)
                .name("Pizza")
                .description("Large pizza")
                .cost(new BigDecimal("1500.00"))
                .splitCost(true)
                .completed(false)
                .build();
        contribution.setId(contributionId);
    }

    @Nested
    @DisplayName("createContribution")
    class CreateContribution {

        @Test
        @DisplayName("should create contribution successfully as organizer")
        void shouldCreateContributionSuccessfullyAsOrganizer() {
            CreateContributionRequestDTO request = new CreateContributionRequestDTO();
            request.setName("Pizza");
            request.setDescription("Large pizza");
            request.setCost(new BigDecimal("1500.00"));
            request.setSplitCost(true);

            when(currentUserService.getCurrentUser()).thenReturn(organizer);
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
            when(contributionRepository.save(any())).thenReturn(contribution);
            when(invitationService.getAcceptedParticipantIds(event)).thenReturn(List.of());

            ApiResponseDTO<Void> response = contributionService.createContribution(eventId, request);

            assertThat(response.isSuccess()).isTrue();
            verify(contributionRepository).save(any(Contribution.class));
            verify(eventProducer).sendNotification(any());
        }

        @Test
        @DisplayName("should create contribution as accepted participant")
        void shouldCreateContributionAsAcceptedParticipant() {
            CreateContributionRequestDTO request = new CreateContributionRequestDTO();
            request.setName("Drinks");
            request.setSplitCost(false);

            EventInvitation invitation = EventInvitation.builder()
                    .event(event)
                    .invitedUser(participant)
                    .invitedBy(organizer)
                    .status(InvitationStatus.ACCEPTED)
                    .build();

            when(currentUserService.getCurrentUser()).thenReturn(participant);
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
            when(invitationRepository.findByEventAndInvitedUser(event, participant))
                    .thenReturn(Optional.of(invitation));
            when(contributionRepository.save(any())).thenReturn(contribution);
            when(invitationService.getAcceptedParticipantIds(event)).thenReturn(List.of());

            ApiResponseDTO<Void> response = contributionService.createContribution(eventId, request);

            assertThat(response.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("should throw when event not found")
        void shouldThrowWhenEventNotFound() {
            when(currentUserService.getCurrentUser()).thenReturn(organizer);
            when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

            CreateContributionRequestDTO request = new CreateContributionRequestDTO();

            assertThatThrownBy(() -> contributionService.createContribution(eventId, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("eventNotFound");
        }

        @Test
        @DisplayName("should throw when user is not part of event")
        void shouldThrowWhenUserNotPartOfEvent() {
            CreateContributionRequestDTO request = new CreateContributionRequestDTO();
            request.setName("Pizza");

            when(currentUserService.getCurrentUser()).thenReturn(participant);
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
            when(invitationRepository.findByEventAndInvitedUser(event, participant))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> contributionService.createContribution(eventId, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("notPartOfEvent");
        }

        @Test
        @DisplayName("should throw when participant not accepted")
        void shouldThrowWhenParticipantNotAccepted() {
            CreateContributionRequestDTO request = new CreateContributionRequestDTO();
            request.setName("Pizza");

            EventInvitation invitation = EventInvitation.builder()
                    .event(event)
                    .invitedUser(participant)
                    .invitedBy(organizer)
                    .status(InvitationStatus.PENDING)
                    .build();

            when(currentUserService.getCurrentUser()).thenReturn(participant);
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
            when(invitationRepository.findByEventAndInvitedUser(event, participant))
                    .thenReturn(Optional.of(invitation));

            assertThatThrownBy(() -> contributionService.createContribution(eventId, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("onlyAcceptedCanManage");
        }
    }

    @Nested
    @DisplayName("updateContribution")
    class UpdateContribution {

        @Test
        @DisplayName("should update contribution successfully as owner")
        void shouldUpdateContributionSuccessfullyAsOwner() {
            CreateContributionRequestDTO request = new CreateContributionRequestDTO();
            request.setName("Updated Pizza");
            request.setCost(new BigDecimal("2000.00"));
            request.setSplitCost(true);

            EventInvitation invitation = EventInvitation.builder()
                    .event(event)
                    .invitedUser(participant)
                    .invitedBy(organizer)
                    .status(InvitationStatus.ACCEPTED)
                    .build();

            when(currentUserService.getCurrentUser()).thenReturn(participant);
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
            when(contributionRepository.findById(contributionId)).thenReturn(Optional.of(contribution));
            when(invitationRepository.findByEventAndInvitedUser(event, participant))
                    .thenReturn(Optional.of(invitation));
            when(contributionRepository.save(any())).thenReturn(contribution);
            when(invitationService.getAcceptedParticipantIds(event)).thenReturn(List.of());

            ApiResponseDTO<Void> response = contributionService
                    .updateContribution(eventId, contributionId, request);

            assertThat(response.isSuccess()).isTrue();
            assertThat(contribution.getName()).isEqualTo("Updated Pizza");
            verify(contributionRepository).save(contribution);
            verify(eventProducer).sendNotification(any());
        }

        @Test
        @DisplayName("should throw when contribution not in event")
        void shouldThrowWhenContributionNotInEvent() {
            Event otherEvent = Event.builder()
                    .title("Other Event")
                    .createdBy(organizer)
                    .status(EventStatus.ACTIVE)
                    .build();
            otherEvent.setId(UUID.randomUUID());

            contribution.setEvent(otherEvent);

            when(currentUserService.getCurrentUser()).thenReturn(organizer);
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
            when(contributionRepository.findById(contributionId)).thenReturn(Optional.of(contribution));

            CreateContributionRequestDTO request = new CreateContributionRequestDTO();

            assertThatThrownBy(() -> contributionService.updateContribution(eventId, contributionId, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("contributionNotInEvent");
        }

        @Test
        @DisplayName("should throw when user cannot edit contribution")
        void shouldThrowWhenUserCannotEditContribution() {
            User anotherUser = User.builder()
                    .firstName("Pedro")
                    .lastName("Lopez")
                    .email("pedro@test.com")
                    .build();
            anotherUser.setId(UUID.randomUUID());

            when(currentUserService.getCurrentUser()).thenReturn(anotherUser);
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
            when(contributionRepository.findById(contributionId)).thenReturn(Optional.of(contribution));

            CreateContributionRequestDTO request = new CreateContributionRequestDTO();

            assertThatThrownBy(() -> contributionService.updateContribution(eventId, contributionId, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("cannotEditContribution");
        }
    }

    @Nested
    @DisplayName("updateContributionStatus")
    class UpdateContributionStatus {

        @Test
        @DisplayName("should mark contribution as completed")
        void shouldMarkContributionAsCompleted() {
            UpdateContributionStatusRequestDTO request = new UpdateContributionStatusRequestDTO();
            request.setCompleted(true);

            when(currentUserService.getCurrentUser()).thenReturn(organizer);
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
            when(contributionRepository.findById(contributionId)).thenReturn(Optional.of(contribution));
            when(contributionRepository.save(any())).thenReturn(contribution);
            when(invitationService.getAcceptedParticipantIds(event)).thenReturn(List.of());

            ApiResponseDTO<Void> response = contributionService
                    .updateContributionStatus(eventId, contributionId, request);

            assertThat(response.isSuccess()).isTrue();
            assertThat(contribution.isCompleted()).isTrue();
            verify(eventProducer).sendNotification(any());
        }

        @Test
        @DisplayName("should not send notification when marking as incomplete")
        void shouldNotSendNotificationWhenMarkingAsIncomplete() {
            contribution.setCompleted(true);

            UpdateContributionStatusRequestDTO request = new UpdateContributionStatusRequestDTO();
            request.setCompleted(false);

            when(currentUserService.getCurrentUser()).thenReturn(organizer);
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
            when(contributionRepository.findById(contributionId)).thenReturn(Optional.of(contribution));
            when(contributionRepository.save(any())).thenReturn(contribution);

            contributionService.updateContributionStatus(eventId, contributionId, request);

            verify(eventProducer, never()).sendNotification(any());
        }
    }

    @Nested
    @DisplayName("deleteContribution")
    class DeleteContribution {

        @Test
        @DisplayName("should delete contribution successfully as organizer")
        void shouldDeleteContributionSuccessfullyAsOrganizer() {
            when(currentUserService.getCurrentUser()).thenReturn(organizer);
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
            when(contributionRepository.findById(contributionId)).thenReturn(Optional.of(contribution));
            when(invitationService.getAcceptedParticipantIds(event)).thenReturn(List.of());

            ApiResponseDTO<Void> response = contributionService
                    .deleteContribution(eventId, contributionId);

            assertThat(response.isSuccess()).isTrue();
            verify(contributionRepository).delete(contribution);
            verify(eventProducer).sendNotification(any());
        }

        @Test
        @DisplayName("should throw when contribution not found")
        void shouldThrowWhenContributionNotFound() {
            when(currentUserService.getCurrentUser()).thenReturn(organizer);
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
            when(contributionRepository.findById(contributionId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> contributionService.deleteContribution(eventId, contributionId))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("contributionNotFound");
        }
    }
}