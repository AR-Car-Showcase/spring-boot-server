package com.arcarshowcaseserver.payload.response;

public record PasswordResetResponse(
        String message,
        boolean resetRequired,
        String email,
        int expiresInMinutes,
        int resendAfterSeconds
) {
}
