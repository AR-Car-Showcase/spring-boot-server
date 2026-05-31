package com.arcarshowcaseserver.configuration;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class PasswordResetSecurityProperties {

    @Value("${app.password-reset.enabled:true}")
    private boolean enabled;

    @Value("${app.password-reset.otp-length:6}")
    private int otpLength;

    @Value("${app.password-reset.otp-ttl-minutes:15}")
    private int otpTtlMinutes;

    @Value("${app.password-reset.resend-cooldown-seconds:60}")
    private int resendCooldownSeconds;

    @Value("${app.password-reset.max-attempts:5}")
    private int maxAttempts;

    @Value("${app.password-reset.store-mode:INMEMORY}")
    private String storeMode;

    @Value("${app.password-reset.redis.key-prefix:security:password-reset}")
    private String redisKeyPrefix;

    @Value("${app.password-reset.smtp-enabled:false}")
    private boolean smtpEnabled;

    @Value("${app.password-reset.from-address:no-reply@arcarshowcase.local}")
    private String fromAddress;
}
