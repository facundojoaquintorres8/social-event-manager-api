package com.socialeventmanager.token.repository;

import com.socialeventmanager.token.entity.Token;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TokenRepository extends JpaRepository<Token, UUID> {

    List<Token> findAllByUserIdAndExpiredFalseAndRevokedFalse(UUID userId);
}