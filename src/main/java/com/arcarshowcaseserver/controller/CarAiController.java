package com.arcarshowcaseserver.controller;

import com.arcarshowcaseserver.dto.ai.CarAiCompareRequest;
import com.arcarshowcaseserver.dto.ai.CarAiRecommendRequest;
import com.arcarshowcaseserver.dto.ai.CarAiResponse;
import com.arcarshowcaseserver.security.services.UserDetailsImpl;
import com.arcarshowcaseserver.service.CarAiAssistantService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cars/ai")
public class CarAiController {

    private final CarAiAssistantService carAiAssistantService;

    public CarAiController(CarAiAssistantService carAiAssistantService) {
        this.carAiAssistantService = carAiAssistantService;
    }

    @PostMapping("/compare")
    public ResponseEntity<CarAiResponse> compareCars(
            @RequestBody CarAiCompareRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        Long userId = userDetails != null ? userDetails.getId() : null;
        return ResponseEntity.ok(carAiAssistantService.compareCars(request, userId));
    }

    @PostMapping("/recommend")
    public ResponseEntity<CarAiResponse> recommendCars(
            @RequestBody CarAiRecommendRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        Long userId = userDetails != null ? userDetails.getId() : null;
        return ResponseEntity.ok(carAiAssistantService.recommendCars(request, userId));
    }
}
