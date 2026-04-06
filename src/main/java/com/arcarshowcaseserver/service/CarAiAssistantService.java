package com.arcarshowcaseserver.service;

import com.arcarshowcaseserver.dto.ai.CarAiCompareRequest;
import com.arcarshowcaseserver.dto.ai.CarAiDataSource;
import com.arcarshowcaseserver.dto.ai.CarAiRecommendRequest;
import com.arcarshowcaseserver.dto.ai.CarAiResponse;
import com.arcarshowcaseserver.model.User;
import com.arcarshowcaseserver.repository.UserRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CarAiAssistantService {

    private static final String DOMAIN_KNOWLEDGE = """
            You are an expert automotive advisor for Indian market buyers.
            Decision framework you must use:
            1) Budget fit and value for money
            2) Daily usage suitability (city, highway, mixed)
            3) Running cost signals from fuel type, mileage and segment
            4) Practicality: seating, space and body type suitability
            5) Performance and drivability from available engine and transmission data
            6) Reliability/safety discussion must be limited to provided catalog data only

            Grounding rules:
            - Use only the provided CAR_CATALOG context.
            - If data is missing, explicitly say "Not available in catalog".
            - Do not invent features, prices, safety ratings, NCAP scores or service costs.
            - Give balanced trade-offs, not marketing language.
            """;

    private final ChatClient chatClient;
    private final CarKnowledgeContextService knowledgeContextService;
    private final UserRepository userRepository;

    @Value("${spring.ai.openai.chat.options.model:qwen/qwen3-32b}")
    private String configuredModel;

    @Value("${spring.ai.openai.api-key:}")
    private String apiKey;

    public CarAiAssistantService(
            ChatClient.Builder chatClientBuilder,
            CarKnowledgeContextService knowledgeContextService,
            UserRepository userRepository
    ) {
        this.chatClient = chatClientBuilder.build();
        this.knowledgeContextService = knowledgeContextService;
        this.userRepository = userRepository;
    }

    public CarAiResponse compareCars(CarAiCompareRequest request, Long userId) {
        CarAiDataSource preference = CarAiDataSource.AUTO;

        CarKnowledgeContextService.ResolvedContext context = knowledgeContextService.resolveCars(
                preference,
                request.getCarIds(),
                request.getCarNames(),
                6
        );

        if (context.cars().size() < 2) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "At least 2 cars are required for AI comparison"
            );
        }

        String userNeed = normalizeNeed(request.getUserNeed(),
                "No explicit needs provided. Compare for a balanced daily ownership profile.");

        String prompt = """
                TASK: Differentiate the selected cars and recommend one best fit.

                USER_NEED:
                %s

                USER_PROFILE:
                %s

                CAR_CATALOG:
                %s

                OUTPUT FORMAT:
                - Best Choice: <car name + short reason>
                - Why It Wins: 3 bullets
                - Key Differences: 3 bullets
                - When To Pick The Alternative: 2 bullets
                - Confidence: Low/Medium/High
                Keep total response under 220 words.
                """.formatted(
                userNeed,
                buildUserProfile(userId),
                renderCarsForPrompt(context.cars())
        );

        String fallback = buildComparisonFallback(context.cars(), userNeed);
        String answer = callLlm(prompt, fallback);

        return new CarAiResponse(
                answer,
                configuredModel,
                context.source().name(),
                toSummaries(context.cars())
        );
    }

    public CarAiResponse recommendCars(CarAiRecommendRequest request, Long userId) {
        CarAiDataSource preference = CarAiDataSource.AUTO;
        int maxCars = request.getMaxCars() == null ? 6 : Math.max(3, Math.min(request.getMaxCars(), 10));

        CarKnowledgeContextService.ResolvedContext context = knowledgeContextService.resolveCars(
                preference,
                List.of(),
                List.of(),
                12
        );

        if (context.cars().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No car catalog data available");
        }

        String userNeed = normalizeNeed(request.getUserNeed(),
                "Recommend best all-rounder cars for mixed city/highway usage.");

        String prompt = """
                TASK: Recommend the best cars based on user need.

                USER_NEED:
                %s

                USER_PROFILE:
                %s

                CAR_CATALOG:
                %s

                OUTPUT FORMAT:
                - Top %d Picks (ranked)
                - Why These Cars Match (bullet points)
                - Trade-offs to be aware of
                - One-line final verdict
                Keep total response under 240 words.
                """.formatted(
                userNeed,
                buildUserProfile(userId),
                renderCarsForPrompt(context.cars()),
                maxCars
        );

        String fallback = buildRecommendationFallback(context.cars(), userNeed, maxCars);
        String answer = callLlm(prompt, fallback);

        return new CarAiResponse(
                answer,
                configuredModel,
                context.source().name(),
                toSummaries(context.cars())
        );
    }

    private String callLlm(String userPrompt, String fallback) {
        String safeFallback = sanitizeAssistantText(fallback);
        if (apiKey == null || apiKey.isBlank()) {
            return safeFallback + "\n\nNote: GROQ_API_KEY is missing, so this is a deterministic comparison.";
        }

        try {
            String content = chatClient.prompt()
                    .system(DOMAIN_KNOWLEDGE)
                    .user(userPrompt)
                    .call()
                    .content();

            String cleaned = sanitizeAssistantText(content);
            if (cleaned.isBlank()) {
                return safeFallback;
            }

            return cleaned;
        } catch (Exception ignored) {
            return safeFallback + "\n\nNote: Live LLM response is temporarily unavailable, so this is a rule-based output.";
        }
    }

    private String sanitizeAssistantText(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        String cleaned = text
                .replaceAll("(?is)<think[^>]*>.*?</think>", " ")
                .replaceAll("(?is)</?think[^>]*>", " ")
                .replace("**", "")
                .replace("__", "");

        cleaned = cleaned
                .replaceAll("(?m)[ \\t]+$", "")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();

        return cleaned;
    }

    private String buildComparisonFallback(List<CarKnowledgeContextService.KnowledgeCar> cars, String userNeed) {
        List<CarKnowledgeContextService.KnowledgeCar> ranked = new ArrayList<>(cars);
        ranked.sort(Comparator
                .comparingDouble(CarKnowledgeContextService.KnowledgeCar::rating)
                .thenComparingDouble(c -> -c.maxPriceLakhs()));

        CarKnowledgeContextService.KnowledgeCar best = ranked.get(ranked.size() - 1);
        CarKnowledgeContextService.KnowledgeCar alternate = ranked.get(ranked.size() - 2);

        return """
                Best Choice: %s %s
                Why It Wins:
                - Higher catalog rating among compared options.
                - Strong overall balance for the stated use case: %s
                - Broader value fit considering pricing and segment.
                Key Differences:
                - %s %s: %.1f/5 rating, %.1f-%.1f lakh.
                - %s %s: %.1f/5 rating, %.1f-%.1f lakh.
                - Fuel/body/transmission choices differ by ownership priorities.
                When To Pick The Alternative:
                - If your budget/feature priorities align more with %s %s pricing and type.
                - If its body-type or fuel setup better fits your daily pattern.
                Confidence: Medium
                """.formatted(
                best.brand(),
                best.model(),
                userNeed,
                best.brand(),
                best.model(),
                best.rating(),
                best.minPriceLakhs(),
                best.maxPriceLakhs(),
                alternate.brand(),
                alternate.model(),
                alternate.rating(),
                alternate.minPriceLakhs(),
                alternate.maxPriceLakhs(),
                alternate.brand(),
                alternate.model()
        );
    }

    private String buildRecommendationFallback(
            List<CarKnowledgeContextService.KnowledgeCar> cars,
            String userNeed,
            int maxCars
    ) {
        List<CarKnowledgeContextService.KnowledgeCar> ranked = cars.stream()
                .sorted(Comparator.comparingDouble(CarKnowledgeContextService.KnowledgeCar::rating).reversed())
                .limit(maxCars)
                .toList();

        String picks = ranked.stream()
                .map(car -> "- " + car.brand() + " " + car.model() + " (" + car.rating() + "/5, " +
                        String.format(Locale.ROOT, "%.1f-%.1fL", car.minPriceLakhs(), car.maxPriceLakhs()) + ")")
                .collect(Collectors.joining("\n"));

        return """
                Top %d Picks (ranked):
                %s

                Why These Cars Match:
                - Highest rated options in the current catalog.
                - Broadly balanced price-to-rating profile for: %s
                - Suitable for general mixed usage when exact constraints are still evolving.

                Trade-offs:
                - Final choice should be narrowed by exact budget, fuel preference and transmission comfort.
                - Some advanced attributes may be missing in catalog for a deeper ownership-cost comparison.

                Final verdict: Start with the top 2 options and do a final shortlisting by budget and fuel preference.
                """.formatted(maxCars, picks, userNeed);
    }

    private String buildUserProfile(Long userId) {
        if (userId == null) {
            return "User is not logged in. No stored profile preferences available.";
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return "No stored profile preferences available.";
        }

        return """
                - Favorite brands: %s
                - Preferred body types: %s
                - Preferred fuel types: %s
                - Preferred transmissions: %s
                - Driving condition: %s
                - Max budget: %s
                """.formatted(
                formatSet(user.getFavBrands()),
                formatSet(user.getPreferredBodyTypes()),
                formatSet(user.getPreferredFuelTypes()),
                formatSet(user.getPreferredTransmissions()),
                safe(user.getDrivingCondition()),
                user.getMaxBudget() == null ? "Not set" : String.format(Locale.ROOT, "%.1f Lakhs", user.getMaxBudget())
        );
    }

    private List<CarAiResponse.CarContextSummary> toSummaries(List<CarKnowledgeContextService.KnowledgeCar> cars) {
        return cars.stream()
                .map(car -> new CarAiResponse.CarContextSummary(
                        car.id(),
                        car.brand(),
                        car.model(),
                        car.bodyType(),
                        car.fuelType(),
                        car.transmissionType(),
                        car.minPriceLakhs(),
                        car.maxPriceLakhs(),
                        car.rating()
                ))
                .toList();
    }

    private String renderCarsForPrompt(List<CarKnowledgeContextService.KnowledgeCar> cars) {
        StringBuilder builder = new StringBuilder();

        int index = 1;
        for (CarKnowledgeContextService.KnowledgeCar car : cars) {
            builder.append(index).append(") ")
                    .append(car.brand()).append(" ").append(car.model()).append("\n")
                    .append("   - Body/Fuel/Transmission: ")
                    .append(car.bodyType()).append(" / ")
                    .append(car.fuelType()).append(" / ")
                    .append(car.transmissionType()).append("\n")
                    .append("   - Seating: ").append(car.seatingCapacity()).append("\n")
                    .append("   - Price: ").append(car.priceRange())
                    .append(" (")
                    .append(String.format(Locale.ROOT, "%.1f-%.1fL", car.minPriceLakhs(), car.maxPriceLakhs()))
                    .append(")\n")
                    .append("   - Rating: ").append(car.rating()).append("/5\n")
                    .append("   - Variant highlights: ").append(renderVariantHighlights(car)).append("\n")
                    .append("   - Spec highlights: ").append(renderSpecHighlights(car)).append("\n\n");
            index++;
        }

        return builder.toString();
    }

    private String renderVariantHighlights(CarKnowledgeContextService.KnowledgeCar car) {
        if (car.variants() == null || car.variants().isEmpty()) {
            return "Not available in catalog";
        }

        return car.variants().stream()
                .limit(2)
                .map(v -> v.variant() + " [" + v.fuel() + ", " + v.transmission() + ", " + v.mileage() + "]")
                .collect(Collectors.joining("; "));
    }

    private String renderSpecHighlights(CarKnowledgeContextService.KnowledgeCar car) {
        if (car.specs() == null || car.specs().isEmpty()) {
            return "Not available in catalog";
        }

        List<String> highlights = new ArrayList<>();

        int categoryCount = 0;
        for (var categoryEntry : car.specs().entrySet()) {
            int specCount = 0;
            for (var specEntry : categoryEntry.getValue().entrySet()) {
                highlights.add(categoryEntry.getKey() + ": " + specEntry.getKey() + "=" + specEntry.getValue());
                specCount++;
                if (specCount >= 2) {
                    break;
                }
            }

            categoryCount++;
            if (categoryCount >= 3 || highlights.size() >= 6) {
                break;
            }
        }

        if (highlights.isEmpty()) {
            return "Not available in catalog";
        }

        return String.join("; ", highlights);
    }

    private String normalizeNeed(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private String formatSet(Set<String> values) {
        if (values == null || values.isEmpty()) {
            return "Not set";
        }

        return values.stream()
                .filter(v -> v != null && !v.isBlank())
                .map(String::trim)
                .collect(Collectors.joining(", "));
    }

    private String safe(String value) {
        return (value == null || value.isBlank()) ? "Not set" : value;
    }
}
