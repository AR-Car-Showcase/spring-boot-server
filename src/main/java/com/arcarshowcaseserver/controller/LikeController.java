package com.arcarshowcaseserver.controller;

import com.arcarshowcaseserver.dto.CarDTO;
import com.arcarshowcaseserver.service.LikeService;
import com.sricharan.security.core.annotation.RequirePermission;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/likes")
public class LikeController {
    

    private final LikeService likeService;

    public LikeController(LikeService likeService) {
        this.likeService = likeService;
    }

    @RequirePermission("like:write")
    @PostMapping("/car/{carId}")
    public ResponseEntity<?> likeCar(
            @PathVariable Long carId) {
        if (carId == null){
            return ResponseEntity.badRequest().body("Car id cannot be null");
        }
        likeService.likeCar(carId);
        return ResponseEntity.ok(Map.of("status", "success", "message", "Car liked successfully"));
    }

    @RequirePermission("like:write")
    @DeleteMapping("/car/{carId}")
    public ResponseEntity<?> unlikeCar(
            @PathVariable Long carId) {
        if (carId == null){
            return ResponseEntity.badRequest().body("Car id cannot be null");
        }
        likeService.unlikeCar(carId);
        return ResponseEntity.ok(Map.of("status", "success", "message", "Car unliked successfully"));
    }

    @RequirePermission("like:read")
    @GetMapping("/my-likes")
    public ResponseEntity<Set<CarDTO>> getMyLikes() {
        return ResponseEntity.ok(likeService.getUserLikedCars());
    }

    @RequirePermission("like:read")
    @GetMapping("/check/{carId}")
    public ResponseEntity<?> checkLike(
            @PathVariable Long carId) {
        if (carId == null){
            return ResponseEntity.badRequest().body("Car id cannot be null");
        }
        return ResponseEntity.ok(likeService.hasUserLikedCar(carId));
    }


}
