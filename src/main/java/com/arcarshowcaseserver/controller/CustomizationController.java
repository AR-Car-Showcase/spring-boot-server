package com.arcarshowcaseserver.controller;

import com.arcarshowcaseserver.payload.request.CustomizationRequest;
import com.arcarshowcaseserver.service.CustomizationService;
import com.sricharan.security.core.annotation.RequirePermission;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
