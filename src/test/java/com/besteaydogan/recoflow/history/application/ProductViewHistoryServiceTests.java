package com.besteaydogan.recoflow.history.application;

import java.time.Instant;
import java.util.UUID;

import com.besteaydogan.recoflow.history.infrastructure.ProductView;
import com.besteaydogan.recoflow.history.infrastructure.ProductViewRepository;
import com.besteaydogan.recoflow.messaging.model.ProductViewEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductViewHistoryServiceTests {

    private final ProductViewRepository repository = mock(ProductViewRepository.class);
    private final ProductViewHistoryService service = new ProductViewHistoryService(repository);

    @Test
    void persistsAValidEvent() {
        ProductViewEvent event = event();

        boolean inserted = service.record(event);

        ArgumentCaptor<ProductView> captor = ArgumentCaptor.forClass(ProductView.class);
        verify(repository).saveAndFlush(captor.capture());
        assertThat(inserted).isTrue();
        assertThat(captor.getValue().getMessageId()).isEqualTo(event.messageId());
        assertThat(captor.getValue().getUserId()).isEqualTo(event.userId());
        assertThat(captor.getValue().getProductId()).isEqualTo(event.properties().productId());
        assertThat(captor.getValue().getSource()).isEqualTo(event.context().source());
        assertThat(captor.getValue().getViewedAt()).isEqualTo(event.viewedAt());
    }

    @Test
    void doesNotInsertAnExistingMessageId() {
        ProductViewEvent event = event();
        when(repository.existsByMessageId(event.messageId())).thenReturn(true);

        boolean inserted = service.record(event);

        assertThat(inserted).isFalse();
        verify(repository, never()).saveAndFlush(org.mockito.ArgumentMatchers.any(ProductView.class));
    }

    private ProductViewEvent event() {
        return new ProductViewEvent(
                "ProductView",
                UUID.fromString("6b1291ea-e50d-425b-9940-44c2aff089c1"),
                "user-78",
                new ProductViewEvent.ProductProperties("product-173"),
                new ProductViewEvent.EventContext("desktop"),
                Instant.parse("2026-07-29T10:15:30Z")
        );
    }
}
