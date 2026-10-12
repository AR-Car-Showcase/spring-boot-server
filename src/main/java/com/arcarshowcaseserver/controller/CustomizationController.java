package com.arcarshowcaseserver.controller;

import com.arcarshowcaseserver.payload.request.CustomizationRequest;
import com.arcarshowcaseserver.service.CustomizationService;
import com.sricharan.security.core.annotation.RequirePermission;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/customizations")
@RequiredArgsConstructor
public class CustomizationController {

    private final CustomizationService customizationService;

    @RequirePermission("customization:write")
    @PostMapping
    public ResponseEntity<?> createCustomization(@RequestBody CustomizationRequest request) {
        return ResponseEntity.ok(customizationService.createCustomization(request));
    }

    @RequirePermission("customization:read")
    @GetMapping
    public ResponseEntity<?> getUserCustomizations() {
        return ResponseEntity.ok(customizationService.getUserCustomizations());
    }

    // Reuses customization:write rather than introducing a new permission value, because
    // permissions are persisted per user at signup and existing accounts would not have it.
    @RequirePermission("customization:write")
    @DeleteMapping("/{customizationId}")
    public ResponseEntity<?> deleteCustomization(@PathVariable UUID customizationId) {
        customizationService.deleteCustomization(customizationId);
        return ResponseEntity.noContent().build();
    }
}
