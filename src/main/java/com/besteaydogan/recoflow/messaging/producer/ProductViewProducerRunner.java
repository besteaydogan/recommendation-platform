package com.besteaydogan.recoflow.messaging.producer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import com.besteaydogan.recoflow.common.config.ProductViewProducerProperties;
import com.besteaydogan.recoflow.messaging.model.ProductViewEvent;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "recoflow.producer", name = "enabled", havingValue = "true")
public class ProductViewProducerRunner implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductViewProducerRunner.class);

    private final ProductViewProducerProperties properties;
    private final ProductViewFileReader fileReader;
    private final ProductViewProducer producer;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(
            Thread.ofPlatform().name("product-view-producer").factory()
    );

    public ProductViewProducerRunner(
            ProductViewProducerProperties properties,
            ProductViewFileReader fileReader,
            ProductViewProducer producer
    ) {
        this.properties = properties;
        this.fileReader = fileReader;
        this.producer = producer;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        Stream<ProductViewEvent> events = fileReader.stream(properties.filePath());
        LOGGER.info("Started streaming product-view source records from {}", properties.filePath());
        try {
            executor.execute(() -> {
                try (events) {
                    producer.publish(events);
                } catch (RuntimeException exception) {
                    LOGGER.error("Product-view publication run failed", exception);
                }
            });
        } catch (RuntimeException exception) {
            events.close();
            throw exception;
        }
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }
}
