package com.arcarshowcaseserver.service.verification;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
public class EmailVerificationChallengeState {

    private String email;
    private Long userId;
    private String codeHash;
    private Instant expiresAt;
    private Instant lastSentAt;
    private int attemptCount;
    private Instant consumedAt;

    public boolean isExpired(Instant now) {
        return expiresAt != null && now != null && now.isAfter(expiresAt);
    }

    public boolean isConsumed() {
        return consumedAt != null;
    }
}
