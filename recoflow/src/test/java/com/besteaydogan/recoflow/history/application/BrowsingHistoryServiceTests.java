package com.besteaydogan.recoflow.history.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.besteaydogan.recoflow.history.api.BrowsingHistoryResponse;
import com.besteaydogan.recoflow.history.infrastructure.ProductView;
import com.besteaydogan.recoflow.history.infrastructure.ProductViewRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BrowsingHistoryServiceTests {

    private final ProductViewRepository repository = mock(ProductViewRepository.class);
    private final BrowsingHistoryService service = new BrowsingHistoryService(repository);

    @Test
    void mapsRepositoryOrderAndRepeatedProductsToPersonalizedResponse() {
        when(repository.findTop10ByUserIdOrderByViewedAtDescIdDesc("user-120"))
                .thenReturn(List.of(
                        view("product-10", "2026-07-29T10:02:00Z"),
                        view("product-10", "2026-07-29T10:01:00Z"),
                        view("product-20", "2026-07-29T10:00:00Z")
                ));

        BrowsingHistoryResponse response = service.getLatest("user-120");

        assertThat(response.userId()).isEqualTo("user-120");
        assertThat(response.products()).containsExactly("product-10", "product-10", "product-20");
        assertThat(response.type()).isEqualTo("personalized");
    }

    @Test
    void mapsMissingHistoryToEmptyPersonalizedResponse() {
        when(repository.findTop10ByUserIdOrderByViewedAtDescIdDesc("missing-user"))
                .thenReturn(List.of());

        BrowsingHistoryResponse response = service.getLatest("missing-user");

        assertThat(response.products()).isEmpty();
        assertThat(response.type()).isEqualTo("personalized");
    }

    @Test
    void deleteDelegatesBothIdentifiers() {
        when(repository.deleteAllByUserIdAndProductId("user-120", "product-10"))
                .thenReturn(3);

        int deleted = service.deleteProduct("user-120", "product-10");

        assertThat(deleted).isEqualTo(3);
        verify(repository).deleteAllByUserIdAndProductId("user-120", "product-10");
    }

    private ProductView view(String productId, String viewedAt) {
        return new ProductView(
                UUID.randomUUID(),
                "user-120",
                productId,
                "test",
                Instant.parse(viewedAt)
        );
    }
}
