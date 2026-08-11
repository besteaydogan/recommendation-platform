package com.besteaydogan.recoflow.messaging.consumer;

import com.besteaydogan.recoflow.common.observability.RecoFlowMetrics;
import com.besteaydogan.recoflow.history.application.ProductViewHistoryService;
import com.besteaydogan.recoflow.messaging.model.ProductViewEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ProductViewConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductViewConsumer.class);

    private final ProductViewHistoryService historyService;
    private final RecoFlowMetrics metrics;

    public ProductViewConsumer(ProductViewHistoryService historyService, RecoFlowMetrics metrics) {
        this.historyService = historyService;
        this.metrics = metrics;
    }

    @KafkaListener(
            topics = "${recoflow.kafka.product-views-topic}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(ProductViewEvent event) {
        metrics.kafkaEventConsumed();
        String invalidReason = invalidReason(event);
        if (invalidReason != null) {
            LOGGER.warn("Skipping invalid product-view event: {}", invalidReason);
            return;
        }

        try {
            boolean inserted = historyService.record(event);
            if (inserted) {
                LOGGER.debug("Stored product-view message {}", event.messageId());
            } else {
                LOGGER.debug("Ignored duplicate product-view message {}", event.messageId());
            }
        } catch (RuntimeException exception) {
            metrics.kafkaConsumerFailed();
            LOGGER.error("Failed to persist product-view message {}", event.messageId(), exception);
            throw exception;
        }
    }

    private String invalidReason(ProductViewEvent event) {
        if (event == null) {
            return "event is null";
        }
        if (!"ProductView".equals(event.event())) {
            return "event type must be ProductView";
        }
        if (event.messageId() == null) {
            return "messageid is required";
        }
        if (isBlank(event.userId())) {
            return "userid is required";
        }
        if (event.properties() == null || isBlank(event.properties().productId())) {
            return "properties.productid is required";
        }
        if (event.context() == null || isBlank(event.context().source())) {
            return "context.source is required";
        }
        if (event.viewedAt() == null) {
            return "viewedat is required";
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
