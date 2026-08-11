package com.besteaydogan.recoflow.history.application;

import java.time.Instant;
import java.util.UUID;

import com.besteaydogan.recoflow.history.infrastructure.ProductViewRepository;
import com.besteaydogan.recoflow.messaging.model.ProductViewEvent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductViewHistoryServiceTests {

    private final ProductViewRepository repository = mock(ProductViewRepository.class);
    private final ProductViewHistoryService service = new ProductViewHistoryService(repository);

    @Test
    void persistsAValidEvent() {
        ProductViewEvent event = event();
        doReturn(1).when(repository).insertIfAbsent(
                event.messageId(),
                event.userId(),
                event.properties().productId(),
                event.context().source(),
                event.viewedAt()
        );

        boolean inserted = service.record(event);

        assertThat(inserted).isTrue();
        verify(repository).insertIfAbsent(
                event.messageId(),
                event.userId(),
                event.properties().productId(),
                event.context().source(),
                event.viewedAt()
        );
    }

    @Test
    void doesNotInsertAnExistingMessageId() {
        ProductViewEvent event = event();
        when(repository.insertIfAbsent(
                event.messageId(),
                event.userId(),
                event.properties().productId(),
                event.context().source(),
                event.viewedAt()
        )).thenReturn(0);

        boolean inserted = service.record(event);

        assertThat(inserted).isFalse();
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
