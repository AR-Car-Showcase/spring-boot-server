package com.arcarshowcaseserver.service.verification;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(prefix = "app.signup.verification", name = "store-mode", havingValue = "INMEMORY", matchIfMissing = true)
public class InMemoryEmailVerificationChallengeStore implements EmailVerificationChallengeStore {

    private final Map<String, EmailVerificationChallengeState> challenges = new ConcurrentHashMap<>();

    @Override
    public Optional<EmailVerificationChallengeState> findByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(challenges.get(normalizedEmail));
    }

    @Override
    public Optional<EmailVerificationChallengeState> findByUserId(Long userId) {
        if (userId == null) {
            return Optional.empty();
        }

        return challenges.values().stream()
                .filter(challenge -> userId.equals(challenge.getUserId()))
                .findFirst();
    }

    @Override
    public void save(EmailVerificationChallengeState challenge) {
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
