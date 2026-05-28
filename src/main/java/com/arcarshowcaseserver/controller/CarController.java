package com.arcarshowcaseserver.controller;

import com.arcarshowcaseserver.dto.CarDTO;
import com.arcarshowcaseserver.dto.InteractionDTO;
import com.arcarshowcaseserver.exceptions.BadRequestException;
import com.arcarshowcaseserver.exceptions.ResourceNotFoundException;
import com.arcarshowcaseserver.model.Cars.Car;
import com.arcarshowcaseserver.security.CurrentAuthenticatedUserService;
import com.arcarshowcaseserver.service.CarService;
import com.arcarshowcaseserver.service.RecommendationService;
import com.sricharan.security.core.annotation.RequirePermission;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/cars")
public class CarController {

    private final CarService carService;
    private final RecommendationService recommendationService;
    private final CurrentAuthenticatedUserService currentAuthenticatedUserService;

    public CarController(CarService carService,
                         RecommendationService recommendationService,
                         CurrentAuthenticatedUserService currentAuthenticatedUserService) {
        this.carService = carService;
        this.recommendationService = recommendationService;
        this.currentAuthenticatedUserService = currentAuthenticatedUserService;
    }

    @GetMapping("/allcars")
    public ResponseEntity<List<Car>> allCars() {
        List<Car> cars = carService.GetAllCars();
        if (cars.isEmpty()) {
            throw new ResourceNotFoundException("No cars found");
        }
        return ResponseEntity.ok(cars);
    }

    @GetMapping("/car/{id}")
    public Car getCarsById(@PathVariable Long id) {
        if (id == null || id <= 0) {
            throw new BadRequestException("Car ID must be a positive number");
        }
        return carService.getCarsById(id);
    }

    @GetMapping("/allBrands")
    public ResponseEntity<List<String>> allBrands() {
        List<String> brands = carService.getAllBrands();
        if (brands.isEmpty()) {
            throw new ResourceNotFoundException("No brands found");
        }
        return ResponseEntity.ok(brands);
    }

    @GetMapping("/{brand}/models")
    public ResponseEntity<?> allModels(@PathVariable String brand) {
        if (brand == null || brand.isBlank()) {
            throw new BadRequestException("Brand name cannot be blank");
        }
        List<?> models = carService.getAllModels(brand);
        if (models.isEmpty()) {
            throw new ResourceNotFoundException("No models found for brand: " + brand);
        }
        return ResponseEntity.ok(models);
    }

    @GetMapping("/{brand}/{model}/variants")
    public ResponseEntity<?> allVariants(
            @PathVariable String brand,
            @PathVariable String model) {
        if (brand == null || brand.isBlank()) {
            throw new BadRequestException("Brand name cannot be blank");
        }
        if (model == null || model.isBlank()) {
            throw new BadRequestException("Model name cannot be blank");
        }
        List<?> variants = carService.getAllVariants(brand, model);
        if (variants.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No variants found for: " + brand + " " + model
            );
        }
        return ResponseEntity.ok(variants);
    }

    @GetMapping("/{brand}/{model}/{variant}")
    public ResponseEntity<?> getVariant(
            @PathVariable String brand,
            @PathVariable String model,
            @PathVariable String variant) {
        if (brand == null || brand.isBlank()) {
            throw new BadRequestException("Brand name cannot be blank");
        }
        if (model == null || model.isBlank()) {
            throw new BadRequestException("Model name cannot be blank");
        }
        if (variant == null || variant.isBlank()) {
            throw new BadRequestException("Variant name cannot be blank");
        }
        return ResponseEntity.ok(carService.getVariant(brand, model, variant));
    }


    @GetMapping("/body-type/{bodyType}")
    public ResponseEntity<List<CarDTO>> getByBodyType(@PathVariable String bodyType) {
        if (bodyType == null || bodyType.isBlank()) {
            throw new BadRequestException("Body type cannot be blank");
        }
        List<CarDTO> cars = carService.getByBodyType(bodyType);
        if (cars == null || cars.isEmpty()) {
            throw new ResourceNotFoundException("No cars found with body type: " + bodyType);
        }
        return ResponseEntity.ok(cars);
    }

    @GetMapping("/brand/{brand}/body-type/{bodyType}")
    public ResponseEntity<List<CarDTO>> getByBrandAndBodyType(
            @PathVariable String brand,
            @PathVariable String bodyType) {
        if (brand == null || brand.isBlank()) {
            throw new BadRequestException("Brand cannot be blank");
        }
        if (bodyType == null || bodyType.isBlank()) {
            throw new BadRequestException("Body type cannot be blank");
        }
        List<CarDTO> cars = carService.getByBrandAndBodyType(brand, bodyType);
        if (cars == null || cars.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No cars found for brand: " + brand + " and body type: " + bodyType
            );
        }
        return ResponseEntity.ok(cars);
    }

    @GetMapping("/fuel-type/{fuelType}")
    public ResponseEntity<List<CarDTO>> getByFuelType(@PathVariable String fuelType) {
        if (fuelType == null || fuelType.isBlank()) {
            throw new BadRequestException("Fuel type cannot be blank");
        }
        List<CarDTO> cars = carService.getByFuelType(fuelType);
        if (cars == null || cars.isEmpty()) {
            throw new ResourceNotFoundException("No cars found with fuel type: " + fuelType);
        }
        return ResponseEntity.ok(cars);
    }

    @GetMapping("/transmission-type/{transmissionType}")
    public ResponseEntity<List<CarDTO>> getByTransmissionType(
            @PathVariable String transmissionType) {
        if (transmissionType == null || transmissionType.isBlank()) {
            throw new BadRequestException("Transmission type cannot be blank");
        }
        List<CarDTO> cars = carService.getBytransmissionType(transmissionType);
        if (cars == null || cars.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No cars found with transmission type: " + transmissionType
            );
        }
        return ResponseEntity.ok(cars);
    }

    @GetMapping("/rating/{rating}")
    public ResponseEntity<List<CarDTO>> getByRating(
            @PathVariable
            @Min(value = 0, message = "Rating must be at least 0")
            @Max(value = 5, message = "Rating must be at most 5")
            double rating) {
        List<CarDTO> cars = carService.getByRating(rating);
        if (cars == null || cars.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No cars found with rating >= " + rating
            );
        }
        return ResponseEntity.ok(cars);
    }

    @GetMapping("/pricing/{pricing}")
    public ResponseEntity<List<CarDTO>> getByPricing(
            @PathVariable
            @Min(value = 1, message = "Price must be at least 1 Lakh")
            @Max(value = 200, message = "Price must be at most 200 Lakhs")
            double pricing) {
        List<CarDTO> cars = carService.getByPricing(pricing);
        if (cars == null || cars.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No cars found under ₹" + pricing + " Lakhs"
            );
        }
        return ResponseEntity.ok(cars);
    }
    @GetMapping("/recommendations/{carId}")
    public ResponseEntity<List<Car>> getRecommendations(@PathVariable Long carId) {
        List<Car> recommendations = recommendationService.getRecommendedCars(carId);
        return ResponseEntity.ok(recommendations);
    }

    @RequirePermission("profile:read")
    @GetMapping("/recommendations/personalized")
    public ResponseEntity<List<Car>> getPersonalizedRecommendations() {
        Long userId = currentAuthenticatedUserService.requireCurrentUserIdAsLong();
        List<Car> recommendations = recommendationService.getPersonalizedRecommendations(userId);
        return ResponseEntity.ok(recommendations);
    }

    @RequirePermission("recommendation:feedback")
    @PostMapping("/recommendations/feedback")
    public ResponseEntity<?> recordFeedback(
            @RequestBody InteractionDTO interaction) {
        Long userId = currentAuthenticatedUserService.requireCurrentUserIdAsLong();
        recommendationService.recordInteraction(userId, interaction.getCarId(), interaction.getAction());
        return ResponseEntity.ok(Map.of("status", "recorded"));
    }

    @GetMapping("/options")
    public ResponseEntity<com.arcarshowcaseserver.dto.CarOptionsDTO> getCarOptions() {
        return ResponseEntity.ok(carService.getCarOptions());
    }
}
