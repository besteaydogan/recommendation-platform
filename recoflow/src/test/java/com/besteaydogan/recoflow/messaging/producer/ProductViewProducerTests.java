package com.besteaydogan.recoflow.messaging.producer;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.besteaydogan.recoflow.common.config.ProductViewKafkaProperties;
import com.besteaydogan.recoflow.common.config.ProductViewProducerProperties;
import com.besteaydogan.recoflow.messaging.model.ProductViewEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductViewProducerTests {

    @Test
    void addsTimestampAndPublishesInSourceOrderUsingUserIdAsKey() throws Exception {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, ProductViewEvent> kafkaTemplate = mock(KafkaTemplate.class);
        when(kafkaTemplate.send(anyString(), anyString(), any(ProductViewEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(null));
        PublicationDelay delay = mock(PublicationDelay.class);
        Instant now = Instant.parse("2026-07-29T10:15:30Z");
        ProductViewProducer producer = new ProductViewProducer(
                kafkaTemplate,
                new ProductViewKafkaProperties("product-views"),
                new ProductViewProducerProperties(true, "ignored.json", Duration.ofSeconds(1)),
                Clock.fixed(now, ZoneOffset.UTC),
                delay
        );
        ProductViewEvent first = sourceEvent(
                "6b1291ea-e50d-425b-9940-44c2aff089c1", "user-78", "product-173");
        ProductViewEvent second = sourceEvent(
                "ca1ff06e-d296-4878-a965-fbf9b3c30d24", "user-20", "product-2");

        producer.publish(List.of(first, second));

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ProductViewEvent> eventCaptor = ArgumentCaptor.forClass(ProductViewEvent.class);
        verify(kafkaTemplate, times(2))
                .send(eq("product-views"), keyCaptor.capture(), eventCaptor.capture());
        assertThat(keyCaptor.getAllValues()).containsExactly("user-78", "user-20");
        assertThat(eventCaptor.getAllValues())
                .extracting(ProductViewEvent::messageId)
                .containsExactly(first.messageId(), second.messageId());
        assertThat(eventCaptor.getAllValues())
                .extracting(ProductViewEvent::viewedAt)
                .containsOnly(now);
        verify(delay).pause(Duration.ofSeconds(1));
        verify(kafkaTemplate).flush();
    }

    private ProductViewEvent sourceEvent(String messageId, String userId, String productId) {
        return new ProductViewEvent(
                "ProductView",
                UUID.fromString(messageId),
                userId,
                new ProductViewEvent.ProductProperties(productId),
                new ProductViewEvent.EventContext("desktop"),
                null
        );
    }
}
