package com.arcarshowcaseserver.service.verification;

import com.arcarshowcaseserver.configuration.SignupSecurityProperties;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Component
public class DisposableEmailPolicy {

    private static final Set<String> DEFAULT_BLOCKED_DOMAINS = Set.of(
            "mailinator.com",
            "10minutemail.com",
            "tempmail.com",
            "yopmail.com",
            "guerrillamail.com",
            "trashmail.com",
            "throwawaymail.com",
            "fakemail.net",
            "maildrop.cc",
            "dispostable.com",
            "sharklasers.com",
            "moakt.com",
            "fakeinbox.com",
            "mailnesia.com",
            "temp-mail.org",
            "mail.tm",
            "getnada.com"
    );

    private final SignupSecurityProperties properties;

    public DisposableEmailPolicy(SignupSecurityProperties properties) {
        this.properties = properties;
    }

    public boolean isDisposable(String email) {
        String domain = extractDomain(email);
        if (domain == null) {
            return true;
        }

        Set<String> blockedDomains = new HashSet<>(DEFAULT_BLOCKED_DOMAINS);
        blockedDomains.addAll(properties.getDisposableEmailDomains());
        for (String blocked : blockedDomains) {
            if (domain.equals(blocked) || domain.endsWith("." + blocked)) {
                return true;
            }
        }

        return false;
    }

    private String extractDomain(String email) {
        if (email == null) {
            return null;
        }

        int atIndex = email.lastIndexOf('@');
        if (atIndex < 0 || atIndex == email.length() - 1) {
            return null;
        }

        return email.substring(atIndex + 1).trim().toLowerCase(Locale.ROOT);
    }
}
