package com.arcarshowcaseserver.service.verification;

import java.util.Optional;

public interface EmailVerificationChallengeStore {
    Optional<EmailVerificationChallengeState> findByEmail(String email);

    Optional<EmailVerificationChallengeState> findByUserId(Long userId);

    void save(EmailVerificationChallengeState challenge);

    void deleteByEmail(String email);
}
