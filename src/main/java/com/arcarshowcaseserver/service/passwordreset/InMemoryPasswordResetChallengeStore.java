package com.arcarshowcaseserver.service.passwordreset;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(prefix = "app.password-reset", name = "store-mode", havingValue = "INMEMORY", matchIfMissing = true)
public class InMemoryPasswordResetChallengeStore implements PasswordResetChallengeStore {

    private final Map<String, PasswordResetChallengeState> challenges = new ConcurrentHashMap<>();

    @Override
    public Optional<PasswordResetChallengeState> findByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(challenges.get(normalizedEmail));
    }

    @Override
    public Optional<PasswordResetChallengeState> findByUserId(Long userId) {
        if (userId == null) {
            return Optional.empty();
        }

        return challenges.values().stream()
                .filter(challenge -> userId.equals(challenge.getUserId()))
                .findFirst();
    }

    @Override
    public void save(PasswordResetChallengeState challenge) {
        if (challenge == null || challenge.getEmail() == null) {
            return;
        }

        challenges.put(normalizeEmail(challenge.getEmail()), challenge);
    }

    @Override
    public void deleteByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail == null) {
            return;
        }

        challenges.remove(normalizedEmail);
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
