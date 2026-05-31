package com.arcarshowcaseserver.service.passwordreset;

import java.util.Optional;

public interface PasswordResetChallengeStore {
    Optional<PasswordResetChallengeState> findByEmail(String email);

    Optional<PasswordResetChallengeState> findByUserId(Long userId);

    void save(PasswordResetChallengeState challenge);

    void deleteByEmail(String email);
}
