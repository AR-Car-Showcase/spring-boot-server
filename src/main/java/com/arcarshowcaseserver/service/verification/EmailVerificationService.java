package com.arcarshowcaseserver.service.verification;

import com.arcarshowcaseserver.configuration.SignupSecurityProperties;
import com.arcarshowcaseserver.model.User;
import com.arcarshowcaseserver.payload.response.EmailVerificationResponse;
import com.arcarshowcaseserver.payload.response.MessageResponse;
import com.arcarshowcaseserver.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

@Service
public class EmailVerificationService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final EmailVerificationChallengeStore challengeStore;
    private final EmailVerificationSender verificationSender;
    private final SignupSecurityProperties properties;
    private final PasswordEncoder passwordEncoder;

    public EmailVerificationService(UserRepository userRepository,
                                    EmailVerificationChallengeStore challengeStore,
                                    EmailVerificationSender verificationSender,
                                    SignupSecurityProperties properties,
                                    PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.challengeStore = challengeStore;
        this.verificationSender = verificationSender;
        this.properties = properties;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public EmailVerificationResponse issueVerificationCode(User user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is required.");
        }

        if (!properties.isVerificationEnabled()) {
            user.setEmailVerified(true);
            userRepository.save(user);
            return new EmailVerificationResponse(
                    "Signup completed successfully.",
                    false,
                    normalizeEmail(user.getEmail()),
                    0,
                    0
            );
        }

        String email = normalizeEmail(user.getEmail());
        int otpLength = Math.max(4, properties.getOtpLength());
        String code = generateNumericOtp(otpLength);
        Instant now = Instant.now();
        Instant expiresAt = now.plus(Duration.ofMinutes(properties.getOtpTtlMinutes()));

        EmailVerificationChallengeState challenge = new EmailVerificationChallengeState();
        challenge.setEmail(email);
        challenge.setUserId(user.getId());
        challenge.setCodeHash(passwordEncoder.encode(code));
        challenge.setExpiresAt(expiresAt);
        challenge.setLastSentAt(now);
        challenge.setAttemptCount(0);
        challenge.setConsumedAt(null);
        challengeStore.save(challenge);

        verificationSender.sendVerificationCode(email, user.getUsername(), code, Duration.ofMinutes(properties.getOtpTtlMinutes()));

        return new EmailVerificationResponse(
                "Verification code sent to your email. Enter the code to activate your account.",
                true,
                email,
                properties.getOtpTtlMinutes(),
                properties.getResendCooldownSeconds()
        );
    }

    @Transactional
    public EmailVerificationResponse resendVerification(String email) {
        String normalizedEmail = normalizeEmail(email);
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No pending verification found for this email."));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This email is already verified.");
        }

        EmailVerificationChallengeState challenge = challengeStore.findByUserId(user.getId())
                .orElse(null);

        Instant now = Instant.now();
        if (challenge != null && challenge.getLastSentAt() != null) {
            long secondsSinceLastSend = Duration.between(challenge.getLastSentAt(), now).getSeconds();
            if (secondsSinceLastSend < properties.getResendCooldownSeconds()) {
                long retryAfter = properties.getResendCooldownSeconds() - secondsSinceLastSend;
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "Please wait " + retryAfter + " seconds before requesting another code.");
            }
        }

        return issueVerificationCode(user);
    }

    @Transactional
    public MessageResponse verifyEmail(String email, String code) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail == null || code == null || code.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email and verification code are required.");
        }

        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No pending verification found for this email."));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            return new MessageResponse("Email is already verified.");
        }

        EmailVerificationChallengeState challenge = challengeStore.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No pending verification found for this email."));

        Instant now = Instant.now();
        if (challenge.isConsumed()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This verification code has already been used.");
        }

        if (challenge.isExpired(now)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verification code expired. Please request a new code.");
        }

        if (challenge.getAttemptCount() >= properties.getMaxAttempts()) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many invalid attempts. Please request a new verification code.");
        }

        if (!passwordEncoder.matches(code.trim(), challenge.getCodeHash())) {
            challenge.setAttemptCount(challenge.getAttemptCount() + 1);
            challengeStore.save(challenge);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid verification code.");
        }

        user.setEmailVerified(true);
        userRepository.save(user);

        challenge.setConsumedAt(now);
        challengeStore.deleteByEmail(normalizedEmail);

        return new MessageResponse("Email verified successfully. You can now sign in.");
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private String generateNumericOtp(int length) {
        int upperBound = (int) Math.pow(10, length);
        int value = SECURE_RANDOM.nextInt(upperBound);
        return String.format("%0" + length + "d", value);
    }
}
