package com.arcarshowcaseserver.service.verification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConditionalOnProperty(prefix = "app.signup.verification", name = "smtp-enabled", havingValue = "false", matchIfMissing = true)
public class LoggingVerificationEmailSender implements EmailVerificationSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingVerificationEmailSender.class);

    @Override
    public void sendVerificationCode(String toEmail, String username, String code, Duration expiresIn) {
        log.warn("Email verification code for {} ({}) is {} and expires in {} minutes",
                username,
                toEmail,
                code,
                Math.max(1, expiresIn.toMinutes()));
    }
}
