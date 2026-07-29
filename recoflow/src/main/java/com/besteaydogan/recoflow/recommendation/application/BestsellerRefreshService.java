package com.besteaydogan.recoflow.recommendation.application;

import java.util.List;

import com.besteaydogan.recoflow.common.config.BestsellerProperties;
import com.besteaydogan.recoflow.recommendation.infrastructure.BestsellerQueryRepository;
import com.besteaydogan.recoflow.recommendation.infrastructure.BestsellerRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BestsellerRefreshService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BestsellerRefreshService.class);
    private static final int RESULT_LIMIT = 10;

    private final BestsellerQueryRepository repository;
    private final BestsellerCache cache;
    private final BestsellerProperties properties;

    public BestsellerRefreshService(
            BestsellerQueryRepository repository,
            BestsellerCache cache,
            BestsellerProperties properties
    ) {
        this.repository = repository;
        this.cache = cache;
        this.properties = properties;
    }

    public void refreshIfEnabled() {
        if (properties.refreshEnabled()) {
            refreshGeneral();
        }
    }

    public void refreshGeneral() {
        try {
            List<String> productIds = productIds(repository.findGeneralBestsellers(RESULT_LIMIT));
            cache.replaceGeneralProducts(productIds);
            LOGGER.info("Refreshed general bestseller cache with {} products", productIds.size());
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Failed to refresh general bestseller cache; keeping previous value: {}",
                    exception.getMessage()
            );
        }
    }

    public List<String> generalBestsellers() {
        return cache.generalProducts()
                .orElseGet(() -> productIds(repository.findGeneralBestsellers(RESULT_LIMIT)));
    }

    private List<String> productIds(List<BestsellerRow> rows) {
        return rows.stream()
                .map(BestsellerRow::productId)
                .toList();
    }
}
