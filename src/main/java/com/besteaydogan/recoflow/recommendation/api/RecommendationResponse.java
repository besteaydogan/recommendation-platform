package com.besteaydogan.recoflow.recommendation.api;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RecommendationResponse(
        @JsonProperty("user-id") String userId,
        List<String> products,
        RecommendationType type
) {

    public RecommendationResponse {
        products = List.copyOf(products);
    }
}
