package com.socialeventmanager.auth.service;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

import com.socialeventmanager.shared.exception.BadRequestException;
import com.socialeventmanager.user.entity.User;
import com.socialeventmanager.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@RequestScope
public class CurrentUserService {

    private final UserRepository userRepository;

    private User cachedUser;

    public User getCurrentUser() {

        if (cachedUser != null) {
            return cachedUser;
        }

        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        cachedUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found"));

        return cachedUser;
    }
}