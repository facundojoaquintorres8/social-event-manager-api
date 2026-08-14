package com.socialeventmanager.payment.service;

import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.socialeventmanager.payment.dto.CreatePreferenceResponseDTO;
import com.socialeventmanager.shared.dto.ApiResponseDTO;
import com.socialeventmanager.user.entity.User;

public interface PaymentService {
    ApiResponseDTO<CreatePreferenceResponseDTO> createPreference(User user) throws MPException, MPApiException;

    void processWebhook(String type, String dataId) throws MPException, MPApiException;
}