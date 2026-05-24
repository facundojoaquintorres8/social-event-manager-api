package com.socialeventmanager.event.service;

import com.socialeventmanager.event.dto.ExternalInvitationPreviewResponseDTO;
import com.socialeventmanager.shared.dto.ApiResponseDTO;
import com.socialeventmanager.user.entity.User;

public interface ExternalInvitationService {
    void claimExternalInvitations(User user);

    ApiResponseDTO<ExternalInvitationPreviewResponseDTO> getInvitationPreview(String token);
}