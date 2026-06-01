package com.arcarshowcaseserver.service;

import com.arcarshowcaseserver.configuration.ModelAssetProperties;
import com.arcarshowcaseserver.model.Cars.CarImage;
import com.arcarshowcaseserver.model.Customization;
import com.arcarshowcaseserver.model.User;
import com.arcarshowcaseserver.payload.request.CustomizationRequest;
import com.arcarshowcaseserver.payload.response.CustomizationResponse;
import com.arcarshowcaseserver.model.Cars.Car;
import com.arcarshowcaseserver.repository.CarRepository;
import com.arcarshowcaseserver.repository.CustomizationRepository;
import com.arcarshowcaseserver.repository.UserRepository;
import com.arcarshowcaseserver.security.CurrentAuthenticatedUserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomizationService {

    private static final Logger log = LoggerFactory.getLogger(CustomizationService.class);
    private final CustomizationRepository customizationRepository;
    private final CarRepository carRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;
    private final CurrentAuthenticatedUserService currentAuthenticatedUserService;
    private final ModelAssetProperties modelAssetProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${blender.service.url}")
    private String blenderServiceUrl;

    @Transactional
    public CustomizationResponse createCustomization(CustomizationRequest request) {
        User user = getCurrentUser();

        Map<String, String> requestedMaterials = request.getMaterials() == null
                ? Map.of()
                : request.getMaterials();
        if (requestedMaterials.isEmpty()) {
            log.warn("[CustomizationService] Customization requested with empty material map; generated model will match base model.");
        }
        log.info("[CustomizationService] Incoming customization request: vehicleId={}, materialCount={}, materialKeys={}",
                request.getVehicleId(),
                requestedMaterials.size(),
                requestedMaterials.keySet());

        Customization customization = new Customization();
        customization.setUser(user);
        customization.setVehicleId(request.getVehicleId());
        try {
            customization.setMaterials(objectMapper.writeValueAsString(requestedMaterials));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting materials to JSON", e);
        }
        customizationRepository.save(customization);

        String generateUrl = blenderServiceUrl + "/generate";
        
        if (request.getVehicleId() == null || request.getVehicleId().isEmpty()) {
            throw new RuntimeException("Vehicle ID is missing in the request");
        }

        Long carId;
        try {
            carId = Long.parseLong(request.getVehicleId());
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid Vehicle ID format: " + request.getVehicleId() + ". Expected a numeric ID.");
        }

        Car car = carRepository.findById(carId)
                .orElseThrow(() -> new RuntimeException("Car not found in database with ID: " + carId));

        String baseModelUrl = car.getModelUrl();
        
        Map<String, Object> pythonRequest = new HashMap<>();
        pythonRequest.put("base_model", baseModelUrl); 
        pythonRequest.put("materials", requestedMaterials);
        pythonRequest.put("output_name", "car_" + customization.getId().toString());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(pythonRequest, headers);

        log.info("[CustomizationService] Requesting generation from: {}", generateUrl);
        ResponseEntity<Map> response;
        try {
            response = restTemplate.postForEntity(generateUrl, entity, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to connect to Blender service at " + generateUrl + ": " + e.getMessage(), e);
        }
        
        if (response.getBody() == null || !response.getBody().containsKey("model_url")) {
            throw new RuntimeException("Invalid response from Blender service: " + response.getBody());
        }

        String modelFilename = (String) response.getBody().get("model_url");

        String fullModelUrl = modelAssetProperties.buildGeneratedModelUrl(modelFilename);
        customization.setModelUrl(fullModelUrl);
        customizationRepository.save(customization);

        String image = car.getImages().stream()
                .filter(img -> "Exterior".equalsIgnoreCase(img.getType()))
                .map(CarImage::getImageUrl)
                .findFirst()
                .orElse(car.getImages().isEmpty() ? "" : car.getImages().get(0).getImageUrl());

        return new CustomizationResponse(
                customization.getId(),
                fullModelUrl,
                car.getBrand(),
                car.getModel(),
                image,
                car.getId().toString(),
                customization.getMaterials()
        );
    }

    public List<CustomizationResponse> getUserCustomizations() {
        User user = getCurrentUser();
        return customizationRepository.findByUser(user).stream()
                .map(c -> {
                    String brand = "";
                    String model = "";
                    String image = "";
                    try {
                        Long carId = Long.parseLong(c.getVehicleId());
                        Car car = carRepository.findById(carId).orElse(null);
                        if (car != null) {
                            brand = car.getBrand();
                            model = car.getModel();
                            image = car.getImages().stream()
                                    .filter(img -> "Exterior".equalsIgnoreCase(img.getType()))
                                    .map(CarImage::getImageUrl)
                                    .findFirst()
                                    .orElse(car.getImages().isEmpty() ? "" : car.getImages().get(0).getImageUrl());
                        }
                    } catch (NumberFormatException e) {
                        log.error("NumberFormatException while parsing vehicle ID: {}", c.getVehicleId());
                    }
                    return new CustomizationResponse(c.getId(), c.getModelUrl(), brand, model, image, c.getVehicleId(), c.getMaterials());
                })
                .collect(Collectors.toList());
    }

    private User getCurrentUser() {
        Long userId = currentAuthenticatedUserService.requireCurrentUserIdAsLong();
        return userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
