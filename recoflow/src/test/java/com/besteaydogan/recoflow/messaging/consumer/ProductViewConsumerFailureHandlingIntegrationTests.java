package com.besteaydogan.recoflow.messaging.consumer;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.besteaydogan.recoflow.history.application.ProductViewHistoryService;
import com.besteaydogan.recoflow.history.infrastructure.ProductViewRepository;
import com.besteaydogan.recoflow.history.infrastructure.TopCategoryQueryRepository;
import com.besteaydogan.recoflow.messaging.model.ProductViewEvent;
import com.besteaydogan.recoflow.recommendation.infrastructure.BestsellerQueryRepository;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DirtiesContext
@EmbeddedKafka(
        partitions = 1,
        topics = {"product-views", "product-views.DLT"},
        bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
@SpringBootTest(properties = {
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
        "recoflow.kafka.retry-max-attempts=3",
        "recoflow.kafka.retry-backoff=100ms",
        "recoflow.producer.enabled=false",
        "recoflow.bestseller.refresh-enabled=false"
})
class ProductViewConsumerFailureHandlingIntegrationTests {

    private static final String DLT_TOPIC = "product-views.DLT";

    @Autowired
    private KafkaTemplate<String, ProductViewEvent> kafkaTemplate;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @MockitoBean
    private ProductViewHistoryService historyService;

    @MockitoBean
    private ProductViewRepository productViewRepository;

    @MockitoBean
    private TopCategoryQueryRepository topCategoryQueryRepository;

    @MockitoBean
    private BestsellerQueryRepository bestsellerQueryRepository;

    private Consumer<String, ProductViewEvent> dltConsumer;

    @BeforeEach
    void setUp() {
        reset(historyService);
        dltConsumer = createDltConsumer();
        embeddedKafka.consumeFromAnEmbeddedTopic(dltConsumer, DLT_TOPIC);
    }

    @AfterEach
    void tearDown() {
        dltConsumer.close();
    }

    @Test
    void retryableFailureIsRetriedWithBackoffAndPublishedToDlt() throws Exception {
        ProductViewEvent event = validEvent(UUID.randomUUID());
        when(historyService.record(event)).thenThrow(new IllegalStateException("database unavailable"));

        long startedAt = System.nanoTime();
        kafkaTemplate.send("product-views", event.userId(), event).get();

        ConsumerRecord<String, ProductViewEvent> dltRecord =
                KafkaTestUtils.getSingleRecord(dltConsumer, DLT_TOPIC, Duration.ofSeconds(10));
        Duration elapsed = Duration.ofNanos(System.nanoTime() - startedAt);

        verify(historyService, timeout(10_000).times(3)).record(event);
        assertThat(elapsed).isGreaterThanOrEqualTo(Duration.ofMillis(200));
        assertThat(dltRecord.key()).isEqualTo(event.userId());
        assertThat(dltRecord.value()).isEqualTo(event);
    }

    @Test
    void duplicateMessageIdIsHandledOnceAndIsNotPublishedToDlt() throws Exception {
        ProductViewEvent event = validEvent(UUID.randomUUID());
        when(historyService.record(event)).thenThrow(new DataIntegrityViolationException(
                "duplicate",
                new IllegalStateException("constraint uk_product_views_message_id")
        ));

        kafkaTemplate.send("product-views", event.userId(), event).get();

        verify(historyService, timeout(10_000).times(1)).record(event);
        ConsumerRecords<String, ProductViewEvent> records = dltConsumer.poll(Duration.ofSeconds(1));

        assertThat(records.records(DLT_TOPIC))
                .extracting(ConsumerRecord::value)
                .doesNotContain(event);
    }

    private Consumer<String, ProductViewEvent> createDltConsumer() {
        Map<String, Object> properties = KafkaTestUtils.consumerProps(
                "dlt-verification-" + UUID.randomUUID(),
                "false",
                embeddedKafka
        );
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        JsonDeserializer<ProductViewEvent> valueDeserializer =
                new JsonDeserializer<>(ProductViewEvent.class, false);
        valueDeserializer.addTrustedPackages("com.besteaydogan.recoflow.messaging.model");
        return new DefaultKafkaConsumerFactory<>(
                properties,
                new StringDeserializer(),
                valueDeserializer
        ).createConsumer();
    }

    private ProductViewEvent validEvent(UUID messageId) {
        return new ProductViewEvent(
                "ProductView",
                messageId,
                "user-78",
                new ProductViewEvent.ProductProperties("product-173"),
                new ProductViewEvent.EventContext("desktop"),
                Instant.parse("2026-07-29T10:15:30Z")
        );
    }
}
