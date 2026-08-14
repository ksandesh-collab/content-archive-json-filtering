package org.example.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public record PubSeriesGroup(
        String featuredImageUrl,
        int id,
        boolean isFeatured,
        boolean isOnboardingQuizDefault,
        String name,
        List<JsonNode> regions,
        String status
) {
}
