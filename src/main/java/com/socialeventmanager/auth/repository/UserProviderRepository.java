package com.socialeventmanager.auth.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.socialeventmanager.auth.entity.UserProvider;
import com.socialeventmanager.auth.enums.Provider;

public interface UserProviderRepository extends JpaRepository<UserProvider, UUID> {
    Optional<UserProvider> findByProviderAndProviderId(Provider provider, String providerId);

    boolean existsByUserIdAndProvider(UUID userId, Provider provider);
}