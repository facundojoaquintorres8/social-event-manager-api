package com.socialeventmanager.audit.service;

import com.socialeventmanager.kafka.event.LoginAuditEvent;

public interface AuditService {
    void saveLoginAudit(LoginAuditEvent event);
}