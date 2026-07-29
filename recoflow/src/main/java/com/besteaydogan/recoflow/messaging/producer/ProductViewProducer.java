package com.besteaydogan.recoflow.messaging.producer;

import java.time.Clock;
import java.util.List;

import com.besteaydogan.recoflow.common.config.ProductViewKafkaProperties;
import com.besteaydogan.recoflow.common.config.ProductViewProducerProperties;
import com.besteaydogan.recoflow.messaging.model.ProductViewEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class ProductViewProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductViewProducer.class);

    private final KafkaTemplate<String, ProductViewEvent> kafkaTemplate;
    private final ProductViewKafkaProperties kafkaProperties;
    private final ProductViewProducerProperties producerProperties;
    private final Clock clock;
    private final PublicationDelay publicationDelay;

    public ProductViewProducer(
            KafkaTemplate<String, ProductViewEvent> kafkaTemplate,
            ProductViewKafkaProperties kafkaProperties,
            ProductViewProducerProperties producerProperties,
            Clock clock,
            PublicationDelay publicationDelay
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaProperties = kafkaProperties;
        this.producerProperties = producerProperties;
        this.clock = clock;
        this.publicationDelay = publicationDelay;
    }

    public void publish(List<ProductViewEvent> sourceEvents) {
        for (int index = 0; index < sourceEvents.size(); index++) {
            ProductViewEvent event = withCurrentTimestamp(sourceEvents.get(index));
            publish(event);
            if (index < sourceEvents.size() - 1 && !pause()) {
                break;
            }
        }
        kafkaTemplate.flush();
        LOGGER.info("Product-view publication run completed");
    }

    private ProductViewEvent withCurrentTimestamp(ProductViewEvent source) {
        return new ProductViewEvent(
                source.event(),
                source.messageId(),
                source.userId(),
                source.properties(),
                source.context(),
                clock.instant()
        );
    }

    private void publish(ProductViewEvent event) {
        try {
            kafkaTemplate.send(kafkaProperties.productViewsTopic(), event.userId(), event)
                    .whenComplete((result, exception) -> {
                        if (exception == null) {
                            LOGGER.info("Published product-view message {}", event.messageId());
                        } else {
                            LOGGER.error("Failed to publish product-view message {}", event.messageId(), exception);
                        }
                    });
        } catch (RuntimeException exception) {
            LOGGER.error("Failed to publish product-view message {}", event.messageId(), exception);
        }
    }

    private boolean pause() {
        try {
            publicationDelay.pause(producerProperties.interval());
            return true;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            LOGGER.info("Product-view publication interrupted");
            return false;
        }
    }
}
