package com.socialeventmanager.audit.service;

import org.springframework.stereotype.Service;

import com.socialeventmanager.audit.entity.LoginAudit;
import com.socialeventmanager.audit.repository.LoginAuditRepository;
import com.socialeventmanager.kafka.event.LoginAuditEvent;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final LoginAuditRepository loginAuditRepository;

    @Override
    public void saveLoginAudit(LoginAuditEvent event) {
        LoginAudit audit = LoginAudit.builder()
                .userId(event.userId())
                .email(event.email())
                .ipAddress(event.ipAddress())
                .userAgent(event.userAgent())
                .success(event.success())
                .failureReason(event.failureReason())
                .build();

        loginAuditRepository.save(audit);
    }
}