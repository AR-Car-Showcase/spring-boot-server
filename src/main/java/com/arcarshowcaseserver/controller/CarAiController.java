package com.arcarshowcaseserver.controller;

import com.arcarshowcaseserver.dto.ai.CarAiCompareRequest;
import com.arcarshowcaseserver.dto.ai.CarAiRecommendRequest;
import com.arcarshowcaseserver.dto.ai.CarAiResponse;
import com.arcarshowcaseserver.security.CurrentAuthenticatedUserService;
import com.arcarshowcaseserver.service.CarAiAssistantService;
import com.sricharan.security.core.annotation.RequirePermission;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cars/ai")
public class CarAiController {

    private final CarAiAssistantService carAiAssistantService;
    private final CurrentAuthenticatedUserService currentAuthenticatedUserService;

    public CarAiController(CarAiAssistantService carAiAssistantService,
                           CurrentAuthenticatedUserService currentAuthenticatedUserService) {
        this.carAiAssistantService = carAiAssistantService;
        this.currentAuthenticatedUserService = currentAuthenticatedUserService;
    }

    @RequirePermission("cars:ai:compare")
    @PostMapping("/compare")
    public ResponseEntity<CarAiResponse> compareCars(
            @RequestBody CarAiCompareRequest request
    ) {
        Long userId = currentAuthenticatedUserService.requireCurrentUserIdAsLong();
        return ResponseEntity.ok(carAiAssistantService.compareCars(request, userId));
    }

    @RequirePermission("cars:ai:recommend")
    @PostMapping("/recommend")
    public ResponseEntity<CarAiResponse> recommendCars(
            @RequestBody CarAiRecommendRequest request
    ) {
        Long userId = currentAuthenticatedUserService.requireCurrentUserIdAsLong();
        return ResponseEntity.ok(carAiAssistantService.recommendCars(request, userId));
    }
}
