package com.arcarshowcaseserver.service.passwordreset;

import java.time.Duration;

public interface PasswordResetSender {
    void sendResetCode(String toEmail, String username, String code, Duration expiresIn);
}
