package com.arcarshowcaseserver.dto.ai;

import java.util.List;

public record CarAiResponse(
        String answer,
        String model,
        String dataSource,
        List<CarContextSummary> carsUsed
) {

    public record CarContextSummary(
            Long id,
            String brand,
            String model,
            String bodyType,
            String fuelType,
            String transmissionType,
            double minPriceLakhs,
            double maxPriceLakhs,
            double rating
    ) {
    }
}
