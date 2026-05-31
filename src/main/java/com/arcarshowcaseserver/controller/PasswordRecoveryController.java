package com.arcarshowcaseserver.controller;

import com.arcarshowcaseserver.payload.request.ChangePasswordRequest;
import com.arcarshowcaseserver.payload.request.ForgotPasswordRequest;
import com.arcarshowcaseserver.payload.request.ResetPasswordRequest;
import com.arcarshowcaseserver.payload.response.MessageResponse;
import com.arcarshowcaseserver.payload.response.PasswordResetResponse;
import com.arcarshowcaseserver.security.CurrentAuthenticatedUserService;
import com.arcarshowcaseserver.service.passwordreset.PasswordResetService;
import com.sricharan.security.core.annotation.RequirePermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class PasswordRecoveryController {

    private final PasswordResetService passwordResetService;
    private final CurrentAuthenticatedUserService currentAuthenticatedUserService;

    @PostMapping("/forgot-password")
    public ResponseEntity<PasswordResetResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(passwordResetService.requestResetCode(request));
    }

    @PostMapping("/resend-password-reset")
    public ResponseEntity<PasswordResetResponse> resendPasswordReset(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(passwordResetService.resendResetCode(request));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(passwordResetService.resetPassword(request));
    }

    @RequirePermission("profile:write")
    @PostMapping("/change-password")
    public ResponseEntity<MessageResponse> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Long userId = currentAuthenticatedUserService.requireCurrentUserIdAsLong();
        return ResponseEntity.ok(passwordResetService.changePassword(userId, request));
    }
}
