package com.arcarshowcaseserver.service.verification;

import java.time.Duration;

public interface EmailVerificationSender {
    void sendVerificationCode(String toEmail, String username, String code, Duration expiresIn);
}
