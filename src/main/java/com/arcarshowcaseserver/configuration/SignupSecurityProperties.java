package com.arcarshowcaseserver.configuration;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Component
public class SignupSecurityProperties {

    @Value("${app.signup.verification.enabled:true}")
    private boolean verificationEnabled;

    @Value("${app.signup.verification.otp-length:6}")
    private int otpLength;

    @Value("${app.signup.verification.otp-ttl-minutes:10}")
    private int otpTtlMinutes;

    @Value("${app.signup.verification.resend-cooldown-seconds:60}")
    private int resendCooldownSeconds;

    @Value("${app.signup.verification.max-attempts:5}")
    private int maxAttempts;

    @Value("${app.signup.verification.store-mode:INMEMORY}")
    private String verificationStoreMode;

    @Value("${app.signup.verification.redis.key-prefix:security:signup-verification}")
    private String verificationRedisKeyPrefix;

    @Value("${app.signup.verification.smtp-enabled:false}")
    private boolean smtpEnabled;

    @Value("${app.signup.verification.from-address:no-reply@arcarshowcase.local}")
    private String fromAddress;

    @Value("${app.signup.disposable-email-domains:}")
    private String disposableEmailDomainsCsv;

    public Set<String> getDisposableEmailDomains() {
        if (disposableEmailDomainsCsv == null || disposableEmailDomainsCsv.isBlank()) {
            return Set.of();
        }

        return Arrays.stream(disposableEmailDomainsCsv.split(","))
                .map(String::trim)
                .filter(domain -> !domain.isBlank())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }
}
