package com.arcarshowcaseserver.payload.response;

public record EmailVerificationResponse(
        String message,
        boolean verificationRequired,
        String email,
        int expiresInMinutes,
        int resendAfterSeconds
) {
}
