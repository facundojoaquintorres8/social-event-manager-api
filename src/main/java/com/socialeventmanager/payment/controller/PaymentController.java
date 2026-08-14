package com.socialeventmanager.payment.controller;

import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.socialeventmanager.payment.dto.CreatePreferenceResponseDTO;
import com.socialeventmanager.payment.service.PaymentService;
import com.socialeventmanager.shared.dto.ApiResponseDTO;
import com.socialeventmanager.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create-preference")
    public ResponseEntity<ApiResponseDTO<CreatePreferenceResponseDTO>> createPreference(
            @AuthenticationPrincipal User user) throws MPException, MPApiException {
        return ResponseEntity.ok(paymentService.createPreference(user));
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "data.id", required = false) String dataId,
            @RequestBody(required = false) Map<String, Object> body) throws MPException, MPApiException {

        if (type == null && body != null) {
            type = (String) body.get("type");
            if (body.containsKey("data")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) body.get("data");
                dataId = String.valueOf(data.get("id"));
            }
        }

        paymentService.processWebhook(type, dataId);
        return ResponseEntity.ok().build();
    }
}