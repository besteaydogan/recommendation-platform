package com.besteaydogan.recoflow.history.api;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BrowsingHistoryResponse(
        @JsonProperty("user-id") String userId,
        List<String> products,
        String type
) {

    public BrowsingHistoryResponse {
        products = List.copyOf(products);
    }
}
