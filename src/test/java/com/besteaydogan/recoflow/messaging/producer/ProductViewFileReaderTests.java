package com.besteaydogan.recoflow.messaging.producer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import com.besteaydogan.recoflow.common.config.ProductViewProducerProperties;
import com.besteaydogan.recoflow.messaging.model.ProductViewEvent;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.ApplicationArguments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

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

        List<ProductViewEvent> events = reader.read(file.toString());

        assertThat(events).hasSize(2);
        assertThat(events.getFirst().userId()).isEqualTo("user-78");
        assertThat(events.getFirst().properties().productId()).isEqualTo("product-173");
        assertThat(events.getFirst().viewedAt()).isNull();
        assertThat(events.getLast().userId()).isEqualTo("user-317");
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
}
