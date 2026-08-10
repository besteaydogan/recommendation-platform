package com.besteaydogan.recoflow.recommendation.infrastructure;

public record BestsellerRow(
        String productId,
        long distinctBuyerCount
) {
}
