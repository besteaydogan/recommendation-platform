package com.besteaydogan.recoflow.messaging.consumer;

import java.time.Instant;
import java.util.UUID;

import com.besteaydogan.recoflow.history.application.ProductViewHistoryService;
import com.besteaydogan.recoflow.messaging.model.ProductViewEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProductViewConsumerTests {

    private ProductViewHistoryService historyService;
    private ProductViewConsumer consumer;

    @BeforeEach
    void setUp() {
        historyService = mock(ProductViewHistoryService.class);
        consumer = new ProductViewConsumer(historyService);
    }

    @Test
    void validEventIsDelegatedForPersistence() {
        ProductViewEvent event = validEvent("product-173");
        when(historyService.record(event)).thenReturn(true);

        consumer.consume(event);

        verify(historyService).record(event);
    }

    @Test
    void duplicateMessageIdIsIgnored() {
        ProductViewEvent event = validEvent("product-173");
        when(historyService.record(event)).thenReturn(false);

        consumer.consume(event);

        verify(historyService).record(event);
    }

    @Test
    void concurrentUniqueConstraintDuplicateIsIgnored() {
        ProductViewEvent event = validEvent("product-173");
        when(historyService.record(event)).thenThrow(new DataIntegrityViolationException(
                "duplicate",
                new IllegalStateException("constraint uk_product_views_message_id")
        ));

        assertThatCode(() -> consumer.consume(event)).doesNotThrowAnyException();

        verify(historyService).record(event);
    }

    @Test
    void invalidEventTypeIsSkipped() {
        ProductViewEvent valid = validEvent("product-173");
        ProductViewEvent invalid = new ProductViewEvent(
                "OtherEvent",
                valid.messageId(),
                valid.userId(),
                valid.properties(),
                valid.context(),
                valid.viewedAt()
        );

        consumer.consume(invalid);

        verifyNoInteractions(historyService);
    }

    @Test
    void missingRequiredFieldIsSkipped() {
        ProductViewEvent valid = validEvent("product-173");
        ProductViewEvent invalid = new ProductViewEvent(
                valid.event(),
                valid.messageId(),
                valid.userId(),
                new ProductViewEvent.ProductProperties(" "),
                valid.context(),
                valid.viewedAt()
        );

        consumer.consume(invalid);

        verify(historyService, never()).record(invalid);
    }

    @Test
    void unknownProductIdIsPreservedBecauseSchemaHasNoProductForeignKey() {
        ProductViewEvent event = validEvent("product-unknown");
        when(historyService.record(event)).thenReturn(true);

        consumer.consume(event);

        verify(historyService).record(event);
    }

    private ProductViewEvent validEvent(String productId) {
        return new ProductViewEvent(
                "ProductView",
                UUID.fromString("6b1291ea-e50d-425b-9940-44c2aff089c1"),
                "user-78",
                new ProductViewEvent.ProductProperties(productId),
                new ProductViewEvent.EventContext("desktop"),
                Instant.parse("2026-07-29T10:15:30Z")
        );
    }
}
