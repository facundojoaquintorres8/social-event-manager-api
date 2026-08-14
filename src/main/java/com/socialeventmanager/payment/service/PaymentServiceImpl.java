package com.socialeventmanager.payment.service;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;
import com.socialeventmanager.payment.dto.CreatePreferenceResponseDTO;
import com.socialeventmanager.shared.dto.ApiResponseDTO;
import com.socialeventmanager.shared.exception.BadRequestException;
import com.socialeventmanager.shared.util.Constants;
import com.socialeventmanager.user.repository.UserRepository;
import com.socialeventmanager.user.entity.User;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final UserRepository userRepository;

    @Value("${mercadopago.access-token}")
    private String accessToken;

    @Value("${mercadopago.public-key}")
    private String publicKey;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.backend-url}")
    private String backendUrl;

    @PostConstruct
    public void init() {
        MercadoPagoConfig.setAccessToken(accessToken);
    }

    @Override
    public ApiResponseDTO<CreatePreferenceResponseDTO> createPreference(User user) throws MPException, MPApiException {
        if (user.isPremium()) {
            throw new BadRequestException("userAlreadyPremium");
        }

        PreferenceItemRequest item = PreferenceItemRequest.builder()
                .id("premium-plan")
                .title("Social Event Manager — Premium Plan")
                .description("Unlimited events and participants")
                .quantity(1)
                .currencyId("ARS")
                .unitPrice(new BigDecimal("7000"))
                .build();

        PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                .success(frontendUrl + "/payment/success")
                .failure(frontendUrl + "/payment/failure")
                .pending(frontendUrl + "/payment/pending")
                .build();

        PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                .items(List.of(item))
                .backUrls(backUrls)
                .autoReturn("approved")
                .externalReference(user.getId().toString())
                .notificationUrl(backendUrl + "/api/v1/payments/webhook")
                .build();

        PreferenceClient client = new PreferenceClient();
        Preference preference = client.create(preferenceRequest);

        return new ApiResponseDTO<>(
                true,
                "Preference created successfully",
                new CreatePreferenceResponseDTO(
                        preference.getId(),
                        preference.getInitPoint(),
                        preference.getSandboxInitPoint(),
                        publicKey));
    }

    @Override
    public void processWebhook(String type, String dataId) throws MPException, MPApiException {
        if (!"payment".equals(type)) {
            return;
        }

        PaymentClient paymentClient = new PaymentClient();
        Payment payment = paymentClient.get(Long.parseLong(dataId));

        if (!"approved".equals(payment.getStatus())) {
            return;
        }

        userRepository.findById(UUID.fromString(payment.getExternalReference()))
                .ifPresent(user -> {
                    user.setPremium(true);
                    user.setPremiumSince(LocalDateTime.now(Constants.TIMEZONE_ARGENTINA));
                    userRepository.save(user);
                });
    }
}