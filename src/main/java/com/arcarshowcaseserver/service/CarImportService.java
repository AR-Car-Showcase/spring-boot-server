package com.arcarshowcaseserver.service;

import com.arcarshowcaseserver.model.Cars.*;
import com.arcarshowcaseserver.repository.CarRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class CarImportService {

    private static final Logger log = LoggerFactory.getLogger(CarImportService.class);

    private final CarRepository carRepository;
    private final ObjectMapper objectMapper;
    private final com.arcarshowcaseserver.configuration.CarModelConfig carModelConfig;
    private final com.arcarshowcaseserver.configuration.ModelAssetProperties modelAssetProperties;

    public CarImportService(CarRepository carRepository,
                            ObjectMapper objectMapper,
                            com.arcarshowcaseserver.configuration.CarModelConfig carModelConfig,
                            com.arcarshowcaseserver.configuration.ModelAssetProperties modelAssetProperties) {
        this.carRepository = carRepository;
        this.objectMapper = objectMapper;
        this.carModelConfig = carModelConfig;
        this.modelAssetProperties = modelAssetProperties;
    }

    @Transactional
    public void importData(InputStream inputStream) throws IOException {
        JsonNode rootNode = objectMapper.readTree(inputStream);

        if (!rootNode.isArray()) {
            log.warn("Root JSON is not an array. Skipping import.");
            return;
        }

        List<Car> carsToSave = new ArrayList<>();
        for (JsonNode carNode : rootNode) {
            try {
                Car car = mapJsonToCar(carNode);
                carsToSave.add(car);
            } catch (Exception e) {
                log.error("Failed to map car node: {} | Error: {}", carNode.path("model").asText(), e.getMessage());
            }
        }
        carRepository.saveAll(carsToSave);
        log.info("Successfully imported {} cars.", carsToSave.size());
    }

    private Car mapJsonToCar(JsonNode node) {
        Car car = new Car();
        car.setBrand(node.path("brand").asText(""));
        car.setModel(node.path("model").asText(""));
        car.setBodyType(node.path("body_type").asText(""));
        car.setFuelType(node.path("fuel_type").asText(""));
        car.setTransmissionType(node.path("transmission_type").asText(""));
        car.setSeatingCapacity(node.path("seating_capacity").asInt(0));
        car.setPriceRange(node.path("price_range").asText(""));
        car.setMinPriceLakhs(node.path("min_price_lakhs").asDouble(0.0));
        car.setMaxPriceLakhs(node.path("max_price_lakhs").asDouble(0.0));
        car.setRating(node.path("rating").asDouble(0.0));

        String assignedModel = resolveModelFile(car.getBrand(), car.getModel());
        car.setModelUrl(modelAssetProperties.buildStaticModelUrl(assignedModel));

        mapSpecs(node, car);

        mapVariants(node, car);

        mapImages(node, car);

        return car;
    }

    private void mapSpecs(JsonNode node, Car car) {
        JsonNode specsNode = node.path("specs");
        if (specsNode.isMissingNode() || specsNode.isEmpty()) return;

        Iterator<Map.Entry<String, JsonNode>> specCategories = specsNode.fields();
        while (specCategories.hasNext()) {
            Map.Entry<String, JsonNode> categoryEntry = specCategories.next();

            String rawCategory = categoryEntry.getKey();
            String normalizedCategory = normalizeCategory(rawCategory, car.getBrand(), car.getModel());

            JsonNode categoryData = categoryEntry.getValue();
            Iterator<Map.Entry<String, JsonNode>> specFields = categoryData.fields();

            while (specFields.hasNext()) {
                Map.Entry<String, JsonNode> specEntry = specFields.next();
                CarDetail detail = new CarDetail(
                        normalizedCategory,
                        specEntry.getKey(),
                        specEntry.getValue().asText(""),
                        car
                );
                car.getDetails().add(detail);
            }
        }
    }

    private void mapVariants(JsonNode node, Car car) {
        JsonNode variantsNode = node.path("variants");
        if (!variantsNode.isArray()) return;

        for (JsonNode vNode : variantsNode) {
            CarVariant variant = new CarVariant();
            variant.setVariant(vNode.path("variant").asText(""));
            variant.setPrice(vNode.path("price").asText(""));
            variant.setEngineCc(vNode.path("engine_cc").asText(""));
            variant.setFuel(vNode.path("fuel").asText(""));
            variant.setTransmission(vNode.path("transmission").asText(""));
            variant.setMileage(vNode.path("mileage").asText(""));
            variant.setCar(car);

            JsonNode keySpecsNode = vNode.path("key_specifications");
            if (keySpecsNode.isArray()) {
                List<String> keySpecs = new ArrayList<>();
                for (JsonNode ks : keySpecsNode) {
                    keySpecs.add(ks.asText(""));
                }
                variant.setKeySpecifications(keySpecs);
            }

            car.getVariants().add(variant);
        }
    }


    private void mapImages(JsonNode node, Car car) {
        JsonNode imagesNode = node.path("images");
        if (imagesNode.isMissingNode()) return;

        if (imagesNode.has("exterior")) {
            for (JsonNode n : imagesNode.get("exterior")) {
                String url = n.asText("");
                if (!url.isBlank()) {
                    car.getImages().add(new CarImage("EXTERIOR", url, car));
                }
            }
        }

        if (imagesNode.has("interior")) {
            for (JsonNode n : imagesNode.get("interior")) {
                String url = n.asText("");
                if (!url.isBlank()) {
                    car.getImages().add(new CarImage("INTERIOR", url, car));
                }
            }
        }

        if (imagesNode.has("colours")) {
            for (JsonNode n : imagesNode.get("colours")) {
                String name = n.path("name").asText("");
                String imageUrl = n.path("image").asText("");
                if (!name.isBlank()) {
                    CarColor color = new CarColor();
                    color.setName(name);
                    color.setImageUrl(imageUrl);
                    color.setCar(car);
                    car.getColors().add(color);
                }
            }
        }
    }

    private String normalizeCategory(String rawCategory, String brand, String model) {
        if (rawCategory == null || rawCategory.isBlank()) return "General";

        String lower = rawCategory.toLowerCase();

        if (lower.startsWith("key spec"))              return "Key Specifications";
        if (lower.contains("specification"))           return "Full Specifications";
        if (lower.contains("feature"))                 return "Features";
        if (lower.contains("safety"))                  return "Safety";
        if (lower.contains("comfort"))                 return "Comfort";
        if (lower.contains("colour") || lower.contains("color")) return "Colours";
        if (lower.contains("dimension"))               return "Dimensions";
        if (lower.contains("engine"))                  return "Engine";
        if (lower.contains("performance"))             return "Performance";
        if (lower.contains("infotainment"))            return "Infotainment";

        String cleaned = rawCategory;
        if (brand != null && !brand.isBlank()) {
            cleaned = cleaned.replaceAll("(?i)" + Pattern.quote(brand), "").trim();
        }
        if (model != null && !model.isBlank()) {
            cleaned = cleaned.replaceAll("(?i)" + Pattern.quote(model), "").trim();
        }

        cleaned = cleaned.replaceAll("\\s+", " ").trim();

        if (cleaned.isBlank()) return "General";

        return Character.toUpperCase(cleaned.charAt(0)) + cleaned.substring(1).toLowerCase();
    }

    private String resolveModelFile(String brand, String model) {
        if (brand == null || model == null) return "car.glb";

        String normalizedBrand = brand.toLowerCase().replaceAll("[^a-z0-9]", "");
        String normalizedModel = model.toLowerCase().replaceAll("[^a-z0-9]", "");

        for (com.arcarshowcaseserver.configuration.CarModelConfig.ModelEntry entry : carModelConfig.getModels().values()) {
            if (entry.getBrand() == null) continue;

            String configBrand = entry.getBrand().toLowerCase().replaceAll("[^a-z0-9]", "");

            if (!normalizedBrand.equals(configBrand)) continue;

            if (entry.getModelNames() == null) continue;

            for (String configModelName : entry.getModelNames()) {
                String normalizedConfigModel = configModelName.toLowerCase().replaceAll("[^a-z0-9]", "");

                log.debug("Checking: {} vs {}", normalizedModel, normalizedConfigModel);

                if (normalizedModel.contains(normalizedConfigModel)) {
                    log.info("Model MATCHED: {} → {}", model, entry.getFile());
                    return entry.getFile();
                }
            }
        }

        log.warn("No model match for: {} {} → Defaulting to car.glb", brand, model);
        return "car.glb";
    }
}
