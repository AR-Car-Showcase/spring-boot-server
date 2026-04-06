package com.arcarshowcaseserver.service;

import com.arcarshowcaseserver.dto.ai.CarAiDataSource;
import com.arcarshowcaseserver.model.Cars.Car;
import com.arcarshowcaseserver.model.Cars.CarDetail;
import com.arcarshowcaseserver.model.Cars.CarVariant;
import com.arcarshowcaseserver.repository.CarRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class CarKnowledgeContextService {

    private static final int MAX_CONTEXT_CARS = 20;

    private final CarRepository carRepository;
    private final ObjectMapper objectMapper;

    private volatile List<KnowledgeCar> jsonCache;

    public CarKnowledgeContextService(CarRepository carRepository, ObjectMapper objectMapper) {
        this.carRepository = carRepository;
        this.objectMapper = objectMapper;
    }

    public ResolvedContext resolveCars(
            CarAiDataSource preference,
            List<Long> carIds,
            List<String> carNames,
            int fallbackLimit
    ) {
        int effectiveLimit = Math.max(1, Math.min(fallbackLimit, MAX_CONTEXT_CARS));

        List<Long> safeIds = carIds != null ? carIds : List.of();
        List<String> safeNames = carNames != null ? carNames : List.of();

        if (preference == CarAiDataSource.DB) {
            try {
                List<KnowledgeCar> dbCars = fromDb(safeIds, safeNames, effectiveLimit);
                return new ResolvedContext(CarAiDataSource.DB, dbCars);
            } catch (Exception ignored) {
                List<KnowledgeCar> jsonCars = fromJson(safeNames, effectiveLimit);
                return new ResolvedContext(CarAiDataSource.JSON, jsonCars);
            }
        }

        if (preference == CarAiDataSource.JSON) {
            List<KnowledgeCar> jsonCars = fromJson(safeNames, effectiveLimit);
            return new ResolvedContext(CarAiDataSource.JSON, jsonCars);
        }

        try {
            List<KnowledgeCar> dbCars = fromDb(safeIds, safeNames, effectiveLimit);
            if (!dbCars.isEmpty()) {
                return new ResolvedContext(CarAiDataSource.DB, dbCars);
            }
        } catch (Exception ignored) {
        }

        List<KnowledgeCar> jsonCars = fromJson(safeNames, effectiveLimit);
        return new ResolvedContext(CarAiDataSource.JSON, jsonCars);
    }

    private List<KnowledgeCar> fromDb(List<Long> carIds, List<String> carNames, int limit) {
        List<Car> allCars = carRepository.findAll();
        if (allCars.isEmpty()) {
            return List.of();
        }

        List<Car> selectedCars = new ArrayList<>();

        if (!carIds.isEmpty()) {
            Map<Long, Car> carsById = new LinkedHashMap<>();
            for (Car car : allCars) {
                if (car.getId() != null) {
                    carsById.put(car.getId(), car);
                }
            }
            for (Long carId : carIds) {
                Car candidate = carsById.get(carId);
                if (candidate != null) {
                    selectedCars.add(candidate);
                }
            }
        }

        if (selectedCars.isEmpty() && !carNames.isEmpty()) {
            selectedCars = selectByNames(allCars, carNames, limit);
        }

        if (selectedCars.isEmpty()) {
            selectedCars = allCars.stream()
                    .sorted(Comparator.comparingDouble(Car::getRating).reversed())
                    .limit(limit)
                    .toList();
        }

        return selectedCars.stream()
                .limit(limit)
                .map(this::toKnowledgeFromDb)
                .toList();
    }

    private List<KnowledgeCar> fromJson(List<String> carNames, int limit) {
        List<KnowledgeCar> allJsonCars = loadJsonCars();
        if (allJsonCars.isEmpty()) {
            return List.of();
        }

        List<KnowledgeCar> selectedCars;
        if (!carNames.isEmpty()) {
            selectedCars = selectKnowledgeByNames(allJsonCars, carNames, limit);
        } else {
            selectedCars = allJsonCars.stream()
                    .sorted(Comparator.comparingDouble(KnowledgeCar::rating).reversed())
                    .limit(limit)
                    .toList();
        }

        return selectedCars.stream().limit(limit).toList();
    }

    private List<Car> selectByNames(List<Car> allCars, List<String> carNames, int limit) {
        Set<Car> selected = new LinkedHashSet<>();

        for (String rawName : carNames) {
            String normalizedName = normalize(rawName);
            if (normalizedName.isBlank()) {
                continue;
            }

            for (Car car : allCars) {
                String normalizedCarName = normalize(car.getBrand() + " " + car.getModel());
                if (normalizedCarName.contains(normalizedName) || normalizedName.contains(normalizedCarName)) {
                    selected.add(car);
                    break;
                }
            }

            if (selected.size() >= limit) {
                break;
            }
        }

        return new ArrayList<>(selected);
    }

    private List<KnowledgeCar> selectKnowledgeByNames(List<KnowledgeCar> allCars, List<String> carNames, int limit) {
        Set<KnowledgeCar> selected = new LinkedHashSet<>();

        for (String rawName : carNames) {
            String normalizedName = normalize(rawName);
            if (normalizedName.isBlank()) {
                continue;
            }

            for (KnowledgeCar car : allCars) {
                String normalizedCarName = normalize(car.brand() + " " + car.model());
                if (normalizedCarName.contains(normalizedName) || normalizedName.contains(normalizedCarName)) {
                    selected.add(car);
                    break;
                }
            }

            if (selected.size() >= limit) {
                break;
            }
        }

        return new ArrayList<>(selected);
    }

    private List<KnowledgeCar> loadJsonCars() {
        if (jsonCache != null) {
            return jsonCache;
        }

        synchronized (this) {
            if (jsonCache != null) {
                return jsonCache;
            }

            List<KnowledgeCar> parsed = new ArrayList<>();
            try (InputStream is = new ClassPathResource("cars_data_final.json").getInputStream()) {
                JsonNode root = objectMapper.readTree(is);
                if (root != null && root.isArray()) {
                    for (JsonNode node : root) {
                        parsed.add(toKnowledgeFromJson(node));
                    }
                }
            } catch (IOException ignored) {
                parsed = List.of();
            }

            jsonCache = parsed;
            return jsonCache;
        }
    }

    private KnowledgeCar toKnowledgeFromDb(Car car) {
        Map<String, Map<String, String>> specs = new LinkedHashMap<>();
        List<CarDetail> details = car.getDetails() != null ? car.getDetails() : List.of();

        for (CarDetail detail : details) {
            String category = safe(detail.getCategory(), "General");
            Map<String, String> categorySpecs = specs.computeIfAbsent(category, ignored -> new LinkedHashMap<>());
            categorySpecs.put(safe(detail.getKey(), "unknown"), safe(detail.getValue(), "N/A"));
        }

        List<KnowledgeVariant> variants = car.getVariants() == null ? List.of() :
                car.getVariants().stream()
                        .limit(3)
                        .map(this::toKnowledgeVariant)
                        .toList();

        return new KnowledgeCar(
                car.getId(),
                safe(car.getBrand(), ""),
                safe(car.getModel(), ""),
                safe(car.getBodyType(), ""),
                safe(car.getFuelType(), ""),
                safe(car.getTransmissionType(), ""),
                car.getSeatingCapacity(),
                safe(car.getPriceRange(), ""),
                car.getMinPriceLakhs(),
                car.getMaxPriceLakhs(),
                car.getRating(),
                specs,
                variants
        );
    }

    private KnowledgeCar toKnowledgeFromJson(JsonNode node) {
        Map<String, Map<String, String>> specs = new LinkedHashMap<>();
        JsonNode specsNode = node.path("specs");
        if (specsNode.isObject()) {
            specsNode.fields().forEachRemaining(categoryEntry -> {
                Map<String, String> categorySpecs = new LinkedHashMap<>();
                categoryEntry.getValue().fields().forEachRemaining(specEntry ->
                        categorySpecs.put(specEntry.getKey(), specEntry.getValue().asText("N/A"))
                );
                specs.put(categoryEntry.getKey(), categorySpecs);
            });
        }

        List<KnowledgeVariant> variants = new ArrayList<>();
        JsonNode variantsNode = node.path("variants");
        if (variantsNode.isArray()) {
            int count = 0;
            for (JsonNode variantNode : variantsNode) {
                variants.add(new KnowledgeVariant(
                        variantNode.path("variant").asText(""),
                        variantNode.path("price").asText(""),
                        variantNode.path("engine_cc").asText(""),
                        variantNode.path("fuel").asText(""),
                        variantNode.path("transmission").asText(""),
                        variantNode.path("mileage").asText("")
                ));
                count++;
                if (count >= 3) {
                    break;
                }
            }
        }

        return new KnowledgeCar(
                null,
                node.path("brand").asText(""),
                node.path("model").asText(""),
                node.path("body_type").asText(""),
                node.path("fuel_type").asText(""),
                node.path("transmission_type").asText(""),
                node.path("seating_capacity").asInt(0),
                node.path("price_range").asText(""),
                node.path("min_price_lakhs").asDouble(0.0),
                node.path("max_price_lakhs").asDouble(0.0),
                node.path("rating").asDouble(0.0),
                specs,
                variants
        );
    }

    private KnowledgeVariant toKnowledgeVariant(CarVariant variant) {
        return new KnowledgeVariant(
                safe(variant.getVariant(), ""),
                safe(variant.getPrice(), ""),
                safe(variant.getEngineCc(), ""),
                safe(variant.getFuel(), ""),
                safe(variant.getTransmission(), ""),
                safe(variant.getMileage(), "")
        );
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }

    private String safe(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }

    public record ResolvedContext(CarAiDataSource source, List<KnowledgeCar> cars) {
    }

    public record KnowledgeCar(
            Long id,
            String brand,
            String model,
            String bodyType,
            String fuelType,
            String transmissionType,
            int seatingCapacity,
            String priceRange,
            double minPriceLakhs,
            double maxPriceLakhs,
            double rating,
            Map<String, Map<String, String>> specs,
            List<KnowledgeVariant> variants
    ) {
    }

    public record KnowledgeVariant(
            String variant,
            String price,
            String engineCc,
            String fuel,
            String transmission,
            String mileage
    ) {
    }
}
