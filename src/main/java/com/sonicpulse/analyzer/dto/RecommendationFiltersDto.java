package com.sonicpulse.analyzer.dto;

public record RecommendationFiltersDto(
        Integer limit,
        Double targetDanceability,
        Double targetEnergy,
        Double targetValence
) {
}
