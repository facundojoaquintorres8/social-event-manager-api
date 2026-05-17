package com.socialeventmanager.event.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.socialeventmanager.event.entity.EventInvitation;
import com.socialeventmanager.event.entity.ExternalInvitation;
import com.socialeventmanager.event.enums.ExternalInvitationStatus;
import com.socialeventmanager.event.enums.InvitationStatus;
import com.socialeventmanager.event.repository.EventInvitationRepository;
import com.socialeventmanager.event.repository.ExternalInvitationRepository;
import com.socialeventmanager.user.entity.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExternalInvitationServiceImpl implements ExternalInvitationService {

    private final ExternalInvitationRepository externalInvitationRepository;

    private final EventInvitationRepository invitationRepository;

    @Override
    public void claimExternalInvitations(User user) {

        List<ExternalInvitation> invitations = externalInvitationRepository
                .findAllByInvitedEmailAndStatus(
                        user.getEmail(),
                        ExternalInvitationStatus.PENDING);

        for (ExternalInvitation externalInvitation : invitations) {

            boolean alreadyExists = invitationRepository
                    .findByEventAndInvitedUser(
                            externalInvitation.getEvent(),
                            user)
                    .isPresent();

            if (!alreadyExists) {

                EventInvitation invitation = EventInvitation.builder()
                        .event(externalInvitation.getEvent())
                        .invitedUser(user)
                        .invitedBy(externalInvitation.getInvitedBy())
                        .status(InvitationStatus.PENDING)
                        .build();

                invitationRepository.save(invitation);
            }

            externalInvitation.setStatus(ExternalInvitationStatus.CLAIMED);
            externalInvitation.setClaimedAt(LocalDateTime.now());
            externalInvitationRepository.save(externalInvitation);
        }
    }
}