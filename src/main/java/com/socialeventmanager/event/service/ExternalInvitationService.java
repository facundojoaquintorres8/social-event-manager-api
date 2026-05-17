package com.socialeventmanager.event.service;

import com.socialeventmanager.user.entity.User;

public interface ExternalInvitationService {
    void claimExternalInvitations(User user);
}