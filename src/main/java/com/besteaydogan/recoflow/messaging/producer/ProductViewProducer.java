package com.besteaydogan.recoflow.messaging.producer;

import java.time.Clock;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Stream;

import com.besteaydogan.recoflow.common.config.ProductViewKafkaProperties;
import com.besteaydogan.recoflow.common.config.ProductViewProducerProperties;
import com.besteaydogan.recoflow.messaging.model.ProductViewEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
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

    public void publish(Stream<ProductViewEvent> sourceEvents) {
        Iterator<ProductViewEvent> iterator = sourceEvents.iterator();
        while (iterator.hasNext()) {
            ProductViewEvent event = withCurrentTimestamp(iterator.next());
            awaitDelivery(publish(event));
            if (iterator.hasNext() && !pause()) {
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

    private CompletableFuture<SendResult<String, ProductViewEvent>> publish(ProductViewEvent event) {
        try {
            return kafkaTemplate.send(kafkaProperties.productViewsTopic(), event.userId(), event)
                    .whenComplete((result, exception) -> {
                        if (exception == null) {
                            LOGGER.debug("Published product-view message {}", event.messageId());
                        } else {
                            LOGGER.error("Failed to publish product-view message {}", event.messageId(), exception);
                        }
                    });
        } catch (RuntimeException exception) {
            LOGGER.error("Failed to publish product-view message {}", event.messageId(), exception);
            throw new ProductViewPublicationException(
                    "Kafka rejected product-view message " + event.messageId() + " before delivery",
                    exception
            );
        }
    }

    private void awaitDelivery(CompletableFuture<SendResult<String, ProductViewEvent>> deliveryResult) {
        try {
            deliveryResult.join();
        } catch (CompletionException exception) {
            throw new ProductViewPublicationException(
                    "Product-view publication run failed because a message was not delivered",
                    exception.getCause()
            );
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
