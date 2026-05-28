package com.arcarshowcaseserver.controller;

import com.arcarshowcaseserver.dto.CarDetailDTO;
import com.arcarshowcaseserver.dto.GroupedSpecsDTO;
import com.arcarshowcaseserver.exceptions.BadRequestException;
import com.arcarshowcaseserver.exceptions.ResourceNotFoundException;
import com.arcarshowcaseserver.service.CarDetailService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cars/{carId}/details")
public class CarDetailsController {

    private final CarDetailService carDetailService;

    public CarDetailsController(CarDetailService carDetailService) {
        this.carDetailService = carDetailService;
    }

    @GetMapping
    public ResponseEntity<List<CarDetailDTO>> getAllDetails(
            @PathVariable Long carId) {
        if (carId == null || carId <= 0) {
            throw new BadRequestException("Car ID must be a positive number");
        }
        List<CarDetailDTO> details = carDetailService.getAllDetails(carId);
        if (details.isEmpty()) {
            throw new ResourceNotFoundException("No details found for car ID: " + carId);
        }
        return ResponseEntity.ok(details);
    }

    @GetMapping("/grouped")
    public ResponseEntity<GroupedSpecsDTO> getGroupedDetails(
            @PathVariable Long carId) {
        if (carId == null || carId <= 0) {
            throw new BadRequestException("Car ID must be a positive number");
        }
        GroupedSpecsDTO grouped = carDetailService.getDetailsGrouped(carId);
        if (grouped.getKeySpecifications().isEmpty() &&
                grouped.getFullSpecifications().isEmpty()) {
            throw new ResourceNotFoundException("No details found for car ID: " + carId);
        }
        return ResponseEntity.ok(grouped);
    }

    @GetMapping("/key-specifications")
    public ResponseEntity<List<CarDetailDTO>> getKeySpecifications(
            @PathVariable Long carId) {
        if (carId == null || carId <= 0) {
            throw new BadRequestException("Car ID must be a positive number");
        }
        List<CarDetailDTO> specs = carDetailService.getKeySpecifications(carId);
        if (specs.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No key specifications found for car ID: " + carId
            );
        }
        return ResponseEntity.ok(specs);
    }

    @GetMapping("/full-specifications")
    public ResponseEntity<List<CarDetailDTO>> getFullSpecifications(
            @PathVariable Long carId) {
        if (carId == null || carId <= 0) {
            throw new BadRequestException("Car ID must be a positive number");
        }
        List<CarDetailDTO> specs = carDetailService.getFullSpecifications(carId);
        if (specs.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No full specifications found for car ID: " + carId
            );
        }
        return ResponseEntity.ok(specs);
    }

    @GetMapping("/categories")
    public ResponseEntity<List<String>> getCategories(
            @PathVariable Long carId) {
        if (carId == null || carId <= 0) {
            throw new BadRequestException("Car ID must be a positive number");
        }
        List<String> categories = carDetailService.getCategories(carId);
        if (categories.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No categories found for car ID: " + carId
            );
        }
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/search")
    public ResponseEntity<List<CarDetailDTO>> searchByKey(
            @PathVariable Long carId,
            @RequestParam(required = false) String key,
            @RequestParam(required = false, defaultValue = "all") String category) {
        if (carId == null || carId <= 0) {
            throw new BadRequestException("Car ID must be a positive number");
        }
        if (key == null || key.isBlank()) {
            throw new BadRequestException("Query param 'key' is required and cannot be blank");
        }
        if (key.length() < 2) {
            throw new BadRequestException("Search key must be at least 2 characters");
        }
        return ResponseEntity.ok(carDetailService.searchByKey(carId, key, category));
    }

}
