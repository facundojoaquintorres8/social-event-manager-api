package com.socialeventmanager.shared.util;

import java.util.regex.Pattern;

import com.socialeventmanager.shared.exception.BadRequestException;

public class EmailValidator {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-z0-9._-]+$");
    private static final Pattern DOMAIN_PATTERN = Pattern.compile("^[a-z0-9.-]+$");

    private EmailValidator() {
    }

    public static void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throwInvalidEmail();
        }

        if (email.chars().anyMatch(Character::isWhitespace)) {
            throwInvalidEmail();
        }

        int atIndex = email.indexOf('@');
        if (atIndex <= 0 || atIndex != email.lastIndexOf('@')) {
            throwInvalidEmail();
        }

        if (email.length() > 254) {
            throwInvalidEmail();
        }

        String username = email.substring(0, atIndex);
        String domain = email.substring(atIndex + 1);

        validateUsername(username);
        validateDomain(domain);

    }

    private static void validateUsername(String username) {
        if (username.length() > 64) {
            throwInvalidEmail();
        }

        if (username.isBlank() || username.startsWith(".") || username.startsWith("-") || username.startsWith("_")
                || !USERNAME_PATTERN.matcher(username).matches() || username.endsWith(".") || username.endsWith("_")
                || username.endsWith("-") || username.contains("..") || username.contains("__")
                || username.contains("--")) {
            throwInvalidEmail();
        }
    }

    private static void validateDomain(String domain) {
        if (domain.isBlank() || domain.startsWith(".") || domain.startsWith("-")
                || !DOMAIN_PATTERN.matcher(domain).matches() || domain.endsWith(".") || domain.endsWith("-")
                || domain.contains("..") || domain.contains("--")) {
            throwInvalidEmail();
        }

        if (domain.indexOf(".") == -1) {
            throwInvalidEmail();
        }

        int lastDot = domain.lastIndexOf('.');
        if (lastDot <= 0 || lastDot == domain.length() - 1) {
            throwInvalidEmail();
        }

        String tld = domain.substring(lastDot + 1);
        if (tld.length() < 2) {
            throwInvalidEmail();
        }

    }

    private static void throwInvalidEmail() {
        throw new BadRequestException("Invalid email");
    }
}
