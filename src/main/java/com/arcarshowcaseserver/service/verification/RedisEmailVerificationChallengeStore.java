package com.arcarshowcaseserver.service.verification;

import com.arcarshowcaseserver.configuration.SignupSecurityProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Component
@ConditionalOnProperty(prefix = "app.signup.verification", name = "store-mode", havingValue = "REDIS")
public class RedisEmailVerificationChallengeStore implements EmailVerificationChallengeStore {

    private static final String FIELD_EMAIL = "email";
    private static final String FIELD_USER_ID = "userId";
    private static final String FIELD_CODE_HASH = "codeHash";
    private static final String FIELD_EXPIRES_AT = "expiresAt";
    private static final String FIELD_LAST_SENT_AT = "lastSentAt";
    private static final String FIELD_ATTEMPT_COUNT = "attemptCount";
    private static final String FIELD_CONSUMED_AT = "consumedAt";

    private final StringRedisTemplate redisTemplate;
    private final SignupSecurityProperties properties;

    public RedisEmailVerificationChallengeStore(StringRedisTemplate redisTemplate,
                                                SignupSecurityProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    @Override
    public Optional<EmailVerificationChallengeState> findByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail == null) {
            return Optional.empty();
        }

        Map<Object, Object> data = redisTemplate.opsForHash().entries(emailKey(normalizedEmail));
        if (data == null || data.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(fromHash(normalizedEmail, data));
    }

    @Override
    public Optional<EmailVerificationChallengeState> findByUserId(Long userId) {
        if (userId == null) {
            return Optional.empty();
        }

        String email = redisTemplate.opsForValue().get(userKey(userId));
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }

        return findByEmail(email);
    }

    @Override
    public void save(EmailVerificationChallengeState challenge) {
        if (challenge == null || challenge.getEmail() == null) {
            return;
        }

        String normalizedEmail = normalizeEmail(challenge.getEmail());
        String emailKey = emailKey(normalizedEmail);
        String userKey = challenge.getUserId() == null ? null : userKey(challenge.getUserId());
        Map<String, String> payload = new HashMap<>();
        payload.put(FIELD_EMAIL, normalizedEmail);
        if (challenge.getUserId() != null) {
            payload.put(FIELD_USER_ID, String.valueOf(challenge.getUserId()));
        }
        if (challenge.getCodeHash() != null) {
            payload.put(FIELD_CODE_HASH, challenge.getCodeHash());
        }
        if (challenge.getExpiresAt() != null) {
            payload.put(FIELD_EXPIRES_AT, challenge.getExpiresAt().toString());
        }
        if (challenge.getLastSentAt() != null) {
            payload.put(FIELD_LAST_SENT_AT, challenge.getLastSentAt().toString());
        }
        payload.put(FIELD_ATTEMPT_COUNT, String.valueOf(challenge.getAttemptCount()));
        if (challenge.getConsumedAt() != null) {
            payload.put(FIELD_CONSUMED_AT, challenge.getConsumedAt().toString());
        }

        redisTemplate.opsForHash().putAll(emailKey, payload);
        Duration ttl = ttlUntilDeletion(challenge.getExpiresAt());
        if (!ttl.isNegative() && !ttl.isZero()) {
            redisTemplate.expire(emailKey, ttl);
            if (userKey != null) {
                redisTemplate.opsForValue().set(userKey, normalizedEmail, ttl);
            }
        }
    }

    @Override
    public void deleteByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail == null) {
            return;
        }

        findByEmail(normalizedEmail).ifPresent(challenge -> {
            redisTemplate.delete(emailKey(normalizedEmail));
            if (challenge.getUserId() != null) {
                redisTemplate.delete(userKey(challenge.getUserId()));
            }
        });
    }

    private EmailVerificationChallengeState fromHash(String email, Map<Object, Object> data) {
        EmailVerificationChallengeState state = new EmailVerificationChallengeState();
        state.setEmail(email);
        state.setUserId(parseLong(data.get(FIELD_USER_ID)));
        state.setCodeHash(asString(data.get(FIELD_CODE_HASH)));
        state.setExpiresAt(parseInstant(data.get(FIELD_EXPIRES_AT)));
        state.setLastSentAt(parseInstant(data.get(FIELD_LAST_SENT_AT)));
        state.setAttemptCount(parseInt(data.get(FIELD_ATTEMPT_COUNT)));
        state.setConsumedAt(parseInstant(data.get(FIELD_CONSUMED_AT)));
        return state;
    }

    private String emailKey(String email) {
        return prefix() + ":email:" + email;
    }

    private String userKey(Long userId) {
        return prefix() + ":user:" + userId;
    }

    private String prefix() {
        String value = properties.getVerificationRedisKeyPrefix();
        return value == null || value.isBlank() ? "security:signup-verification" : value;
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private Duration ttlUntilDeletion(Instant expiresAt) {
        if (expiresAt == null) {
            return Duration.ofHours(1);
        }

        Instant now = Instant.now();
        Duration remaining = Duration.between(now, expiresAt);
        if (remaining.isNegative()) {
            remaining = Duration.ZERO;
        }

        return remaining.plus(Duration.ofHours(1));
    }

    private static String asString(Object value) {
        return value == null ? null : value.toString();
    }

    private static Long parseLong(Object value) {
        try {
            return value == null ? null : Long.parseLong(value.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static int parseInt(Object value) {
        try {
            return value == null ? 0 : Integer.parseInt(value.toString());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private static Instant parseInstant(Object value) {
        try {
            return value == null || value.toString().isBlank() ? null : Instant.parse(value.toString());
        } catch (Exception ex) {
            return null;
        }
    }
}
