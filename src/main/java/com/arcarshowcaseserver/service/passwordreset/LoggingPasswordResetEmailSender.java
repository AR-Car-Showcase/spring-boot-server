package com.arcarshowcaseserver.service.passwordreset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConditionalOnProperty(prefix = "app.password-reset", name = "smtp-enabled", havingValue = "false", matchIfMissing = true)
public class LoggingPasswordResetEmailSender implements PasswordResetSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingPasswordResetEmailSender.class);

    @Override
    public void sendResetCode(String toEmail, String username, String code, Duration expiresIn) {
        log.warn("Password reset code for {} ({}) is {} and expires in {} minutes",
                username,
                toEmail,
                code,
                Math.max(1, expiresIn.toMinutes()));
    }
}
