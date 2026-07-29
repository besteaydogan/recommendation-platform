package com.besteaydogan.recoflow.recommendation.application;

import java.time.Duration;
import java.util.List;

import com.besteaydogan.recoflow.common.config.BestsellerProperties;
import com.besteaydogan.recoflow.recommendation.infrastructure.BestsellerQueryRepository;
import com.besteaydogan.recoflow.recommendation.infrastructure.BestsellerRow;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class BestsellerRefreshServiceTests {

    private final BestsellerQueryRepository repository = mock(BestsellerQueryRepository.class);
    private final BestsellerCache cache = new BestsellerCache();

    @Test
    void successfulRefreshAtomicallyReplacesGeneralCache() {
        when(repository.findGeneralBestsellers(10)).thenReturn(List.of(
                new BestsellerRow("product-1", 10),
                new BestsellerRow("product-2", 9)
        ));
        BestsellerRefreshService service = service(true);

        service.refreshIfEnabled();

        assertThat(cache.generalProducts()).hasValue(List.of("product-1", "product-2"));
    }

    @Test
    void failedRefreshPreservesPreviousCacheValue() {
        cache.replaceGeneralProducts(List.of("product-old"));
        when(repository.findGeneralBestsellers(10)).thenThrow(new IllegalStateException("database unavailable"));
        BestsellerRefreshService service = service(true);

        service.refreshIfEnabled();

        assertThat(cache.generalProducts()).hasValue(List.of("product-old"));
    }

    @Test
    void disabledRefreshDoesNotQueryOrReplaceCache() {
        BestsellerRefreshService service = service(false);

        service.refreshIfEnabled();

        verifyNoInteractions(repository);
        assertThat(cache.generalProducts()).isEmpty();
    }

    private BestsellerRefreshService service(boolean enabled) {
        return new BestsellerRefreshService(
                repository,
                cache,
                new BestsellerProperties(enabled, Duration.ofSeconds(30))
        );
    }
}
