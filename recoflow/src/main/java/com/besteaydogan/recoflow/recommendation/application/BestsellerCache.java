package com.besteaydogan.recoflow.recommendation.application;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Component;

@Component
public class BestsellerCache {

    private final AtomicReference<List<String>> generalProducts = new AtomicReference<>();

    public Optional<List<String>> generalProducts() {
        return Optional.ofNullable(generalProducts.get());
    }

    public void replaceGeneralProducts(List<String> productIds) {
        generalProducts.set(List.copyOf(productIds));
    }
}
