package com.besteaydogan.recoflow.recommendation.api;

import com.fasterxml.jackson.annotation.JsonValue;

public enum RecommendationType {

    PERSONALIZED("personalized"),
    NON_PERSONALIZED("non-personalized");

    private final String jsonValue;

    RecommendationType(String jsonValue) {
        this.jsonValue = jsonValue;
    }

    @JsonValue
    public String jsonValue() {
        return jsonValue;
    }
}
