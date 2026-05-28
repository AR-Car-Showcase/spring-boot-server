package com.arcarshowcaseserver.controller;

import com.arcarshowcaseserver.dto.CarDTO;
import com.arcarshowcaseserver.exceptions.BadRequestException;
import com.arcarshowcaseserver.service.CarSearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/search/cars")
public class CarSearchController {

    private final CarSearchService carSearchService;

    public CarSearchController(CarSearchService carSearchService) {
        this.carSearchService = carSearchService;
    }

    @GetMapping
    public ResponseEntity<List<CarDTO>> searchCars(
            @RequestParam String keyword
    ) {
        if (keyword != null && !keyword.isBlank() && keyword.trim().length() < 2) {
        throw new BadRequestException("Search keyword must be at least 2 characters");
    }
        return ResponseEntity.ok(carSearchService.simpleSearch(keyword));
    }

    @GetMapping("/advanced")
    public ResponseEntity<List<CarDTO>> advancedSearch(
            @RequestParam String keyword
    ) {
        return ResponseEntity.ok(carSearchService.advancedSearch(keyword));
    }

    @GetMapping("/filter")
    public ResponseEntity<List<CarDTO>> multiFilterSearch(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String bodyType,
            @RequestParam(required = false) String fuelType,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) Double minRating
    ) {
        if (minRating != null && (minRating < 0 || minRating > 5)) {
            throw new BadRequestException("minRating must be between 0 and 5");
        }
        return ResponseEntity.ok(
                carSearchService.multiFilterSearch(
                        keyword, brand, bodyType, fuelType,
                        minPrice, maxPrice, minRating
                )
        );
    }
}
