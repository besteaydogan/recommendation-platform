package com.besteaydogan.recoflow.messaging.producer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

import com.besteaydogan.recoflow.common.config.ProductViewProducerProperties;
import com.besteaydogan.recoflow.messaging.model.ProductViewEvent;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.ApplicationArguments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProductViewFileReaderTests {

    private final ProductViewFileReader reader = new ProductViewFileReader(
            JsonMapper.builder().addModule(new JavaTimeModule()).build()
    );

    @TempDir
    Path tempDirectory;

    @Test
    void readsValidJsonLinesAndSkipsMalformedOrIncompleteRecords() throws Exception {
        Path file = tempDirectory.resolve("product-views.json");
        Files.writeString(file, """
                {"event":"ProductView","messageid":"6b1291ea-e50d-425b-9940-44c2aff089c1","userid":"user-78","properties":{"productid":"product-173"},"context":{"source":"desktop"}}
                {"event":
                {"event":"ProductView","messageid":"ca1ff06e-d296-4878-a965-fbf9b3c30d24","userid":"","properties":{"productid":"product-2"},"context":{"source":"mobile-app"}}
                {"event":"ProductView","messageid":"8a52d373-81b9-41b7-b927-35323fa47a6f","userid":"user-317","properties":{"productid":"product-176"},"context":{"source":"mobile-web"}}
                """);

        List<ProductViewEvent> events;
        try (Stream<ProductViewEvent> eventStream = reader.stream(file.toString())) {
            events = eventStream.toList();
        }

        assertThat(events).hasSize(2);
        assertThat(events.getFirst().userId()).isEqualTo("user-78");
        assertThat(events.getFirst().properties().productId()).isEqualTo("product-173");
        assertThat(events.getFirst().viewedAt()).isNull();
        assertThat(events.getLast().userId()).isEqualTo("user-317");
    }

    @Test
    void parsesLinesLazilyInsteadOfMaterializingTheWholeFile() throws Exception {
        Path file = tempDirectory.resolve("lazy-product-views.json");
        Files.writeString(file, "{}\n{}\n");
        com.fasterxml.jackson.databind.ObjectMapper objectMapper =
                mock(com.fasterxml.jackson.databind.ObjectMapper.class);
        ProductViewEvent event = new ProductViewEvent(
                "ProductView",
                java.util.UUID.fromString("6b1291ea-e50d-425b-9940-44c2aff089c1"),
                "user-78",
                new ProductViewEvent.ProductProperties("product-173"),
                new ProductViewEvent.EventContext("desktop"),
                null
        );
        when(objectMapper.readValue(anyString(), eq(ProductViewEvent.class))).thenReturn(event);
        ProductViewFileReader lazyReader = new ProductViewFileReader(objectMapper);

        try (Stream<ProductViewEvent> events = lazyReader.stream(file.toString())) {
            verifyNoInteractions(objectMapper);

            assertThat(events.findFirst()).contains(event);

            verify(objectMapper, times(1)).readValue(anyString(), eq(ProductViewEvent.class));
        }
    }

    @Test
    void enabledRunnerFailsClearlyWhenFileIsMissing() {
        Path missingFile = tempDirectory.resolve("missing.json");
        ProductViewProducerProperties properties = new ProductViewProducerProperties(
                true,
                missingFile.toString(),
                Duration.ofMillis(1)
        );
        ProductViewProducerRunner runner = new ProductViewProducerRunner(
                properties,
                reader,
                mock(ProductViewProducer.class)
        );

        try {
            assertThatThrownBy(() -> runner.run(mock(ApplicationArguments.class)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("missing or unreadable")
                    .hasMessageContaining("missing.json");
        } finally {
            runner.shutdown();
        }
    }

    @Test
    void runnerClosesTheFileStreamAfterPublication() {
        ProductViewFileReader fileReader = mock(ProductViewFileReader.class);
        ProductViewProducer producer = mock(ProductViewProducer.class);
        @SuppressWarnings("unchecked")
        Stream<ProductViewEvent> events = mock(Stream.class);
        ProductViewProducerProperties properties = new ProductViewProducerProperties(
                true,
                "product-views.json",
                Duration.ofMillis(1)
        );
        when(fileReader.stream(properties.filePath())).thenReturn(events);
        ProductViewProducerRunner runner = new ProductViewProducerRunner(properties, fileReader, producer);

        try {
            runner.run(mock(ApplicationArguments.class));

            verify(producer, timeout(1_000)).publish(events);
            verify(events, timeout(1_000)).close();
        } finally {
            runner.shutdown();
        }
    }
}
