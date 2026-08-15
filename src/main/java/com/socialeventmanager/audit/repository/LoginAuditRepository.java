package com.socialeventmanager.audit.repository;

import com.socialeventmanager.audit.entity.LoginAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LoginAuditRepository extends JpaRepository<LoginAudit, UUID> {
}