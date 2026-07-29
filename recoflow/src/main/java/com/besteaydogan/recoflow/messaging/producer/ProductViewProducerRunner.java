package com.besteaydogan.recoflow.messaging.producer;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
        List<ProductViewEvent> events = fileReader.read(properties.filePath());
        LOGGER.info("Loaded {} product-view source records", events.size());
        executor.submit(() -> producer.publish(events));
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }
}
