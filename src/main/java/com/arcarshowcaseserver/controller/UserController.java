package com.arcarshowcaseserver.controller;

import com.arcarshowcaseserver.dto.UserAccountDTO;
import com.arcarshowcaseserver.dto.UserPreferencesDTO;
import com.arcarshowcaseserver.dto.UserProfileDTO;
import com.arcarshowcaseserver.security.CurrentAuthenticatedUserService;
import com.arcarshowcaseserver.service.UserService;
import com.sricharan.security.core.annotation.RequirePermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final CurrentAuthenticatedUserService currentAuthenticatedUserService;

    @RequirePermission("profile:write")
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@Valid @RequestBody UserAccountDTO profileDTO) {
        Long userId = currentAuthenticatedUserService.requireCurrentUserIdAsLong();
        return ResponseEntity.ok(userService.updateProfile(userId, profileDTO));
    }

    @RequirePermission("profile:write")
    @PutMapping("/preferences")
    public ResponseEntity<?> updatePreferences(@RequestBody UserPreferencesDTO preferencesDTO) {
        Long userId = currentAuthenticatedUserService.requireCurrentUserIdAsLong();
        return ResponseEntity.ok(userService.updatePreferences(userId, preferencesDTO));
    }
    
    @RequirePermission("profile:read")
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile() {
        Long userId = currentAuthenticatedUserService.requireCurrentUserIdAsLong();
        return ResponseEntity.ok(userService.getProfile(userId));
    }
}
