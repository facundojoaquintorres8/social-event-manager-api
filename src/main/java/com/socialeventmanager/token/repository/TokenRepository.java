package com.socialeventmanager.token.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.socialeventmanager.token.entity.Token;

public interface TokenRepository extends JpaRepository<Token, UUID> {

    List<Token> findAllByUserIdAndExpiredFalseAndRevokedFalse(UUID userId);
}