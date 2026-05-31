package com.arcarshowcaseserver.service.passwordreset;

import com.arcarshowcaseserver.configuration.PasswordResetSecurityProperties;
import com.arcarshowcaseserver.model.User;
import com.arcarshowcaseserver.payload.request.ChangePasswordRequest;
import com.arcarshowcaseserver.payload.request.ForgotPasswordRequest;
import com.arcarshowcaseserver.payload.request.ResetPasswordRequest;
import com.arcarshowcaseserver.payload.response.MessageResponse;
import com.arcarshowcaseserver.payload.response.PasswordResetResponse;
import com.arcarshowcaseserver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class PasswordResetService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordResetChallengeStore challengeStore;
    private final PasswordResetSender resetSender;
    private final PasswordResetSecurityProperties properties;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public PasswordResetResponse requestResetCode(ForgotPasswordRequest request) {
        String email = normalizeEmail(request.getEmail());
        if (email == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required.");
        }

        User user = findPasswordManagedUser(email);
        if (user != null) {
            issueResetCode(user);
        }

        return new PasswordResetResponse(
                "If an account exists for that email, a password reset code has been sent.",
                true,
                email,
                properties.getOtpTtlMinutes(),
                properties.getResendCooldownSeconds()
        );
    }

    @Transactional
    public PasswordResetResponse resendResetCode(ForgotPasswordRequest request) {
        return requestResetCode(request);
    }

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        String email = normalizeEmail(request.getEmail());
        String code = request.getCode() == null ? null : request.getCode().trim();
        String newPassword = request.getNewPassword();

        if (email == null || code == null || code.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email and reset code are required.");
        }

        User user = findPasswordManagedUser(email);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reset code is invalid or expired.");
        }

        validateAndConsumeChallenge(email, user, code);
        applyNewPassword(user, newPassword);

        return new MessageResponse("Password reset successfully. You can now sign in.");
    }

    @Transactional
    public MessageResponse changePassword(Long userId, ChangePasswordRequest request) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You must be logged in to change your password.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));

        if (user.getPassword() == null || user.getPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Password changes are not available for this account.");
        }

        if (request.getCurrentPassword() == null || request.getCurrentPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is required.");
        }

        if (request.getNewPassword() == null || request.getNewPassword().isBlank() || request.getNewPassword().length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password must be at least 8 characters long.");
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect.");
        }

        applyNewPassword(user, request.getNewPassword());
        return new MessageResponse("Password changed successfully.");
    }

    private void issueResetCode(User user) {
        String email = normalizeEmail(user.getEmail());
        PasswordResetChallengeState existingChallenge = challengeStore.findByUserId(user.getId()).orElse(null);
        if (existingChallenge != null && existingChallenge.getLastSentAt() != null) {
            long secondsSinceLastSend = Duration.between(existingChallenge.getLastSentAt(), Instant.now()).getSeconds();
            if (secondsSinceLastSend < properties.getResendCooldownSeconds()) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "Please wait before requesting another reset code.");
            }
        }

        int otpLength = Math.max(4, properties.getOtpLength());
        String code = generateNumericOtp(otpLength);
        Instant now = Instant.now();
        Instant expiresAt = now.plus(Duration.ofMinutes(properties.getOtpTtlMinutes()));

        PasswordResetChallengeState challenge = new PasswordResetChallengeState();
        challenge.setEmail(email);
        challenge.setUserId(user.getId());
        challenge.setCodeHash(passwordEncoder.encode(code));
        challenge.setExpiresAt(expiresAt);
        challenge.setLastSentAt(now);
        challenge.setAttemptCount(0);
        challenge.setConsumedAt(null);
        challengeStore.save(challenge);

        resetSender.sendResetCode(email, user.getUsername(), code, Duration.ofMinutes(properties.getOtpTtlMinutes()));
    }

    private void validateAndConsumeChallenge(String email, User user, String code) {
        PasswordResetChallengeState challenge = challengeStore.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reset code is invalid or expired."));

        Instant now = Instant.now();
        if (challenge.isConsumed()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This reset code has already been used.");
        }

        if (challenge.isExpired(now)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reset code expired. Please request a new code.");
        }

        if (challenge.getAttemptCount() >= properties.getMaxAttempts()) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many invalid attempts. Please request a new reset code.");
        }

        if (!passwordEncoder.matches(code, challenge.getCodeHash())) {
            challenge.setAttemptCount(challenge.getAttemptCount() + 1);
            challengeStore.save(challenge);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reset code is invalid or expired.");
        }

        challenge.setConsumedAt(now);
        challengeStore.deleteByEmail(email);
    }

    private void applyNewPassword(User user, String newPassword) {
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    private User findPasswordManagedUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .or(() -> userRepository.findByExternalEmail(email))
                .filter(user -> user.getPassword() != null && !user.getPassword().isBlank())
                .orElse(null);
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
