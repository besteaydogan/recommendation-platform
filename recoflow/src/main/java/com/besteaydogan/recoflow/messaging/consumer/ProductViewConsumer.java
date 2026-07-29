package com.besteaydogan.recoflow.messaging.consumer;

import com.besteaydogan.recoflow.history.application.ProductViewHistoryService;
import com.besteaydogan.recoflow.messaging.model.ProductViewEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ProductViewConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductViewConsumer.class);

    private final ProductViewHistoryService historyService;

    public ProductViewConsumer(ProductViewHistoryService historyService) {
        this.historyService = historyService;
    }

    @KafkaListener(
            topics = "${recoflow.kafka.product-views-topic}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(ProductViewEvent event) {
        String invalidReason = invalidReason(event);
        if (invalidReason != null) {
            LOGGER.warn("Skipping invalid product-view event: {}", invalidReason);
            return;
        }

        try {
            boolean inserted = historyService.record(event);
            if (inserted) {
                LOGGER.info("Stored product-view message {}", event.messageId());
            } else {
                LOGGER.info("Ignored duplicate product-view message {}", event.messageId());
            }
        } catch (DataIntegrityViolationException exception) {
            if (isMessageIdDuplicate(exception)) {
                LOGGER.info("Ignored concurrently duplicated product-view message {}", event.messageId());
                return;
            }
            LOGGER.error("Failed to persist product-view message {}", event.messageId(), exception);
            throw exception;
        } catch (RuntimeException exception) {
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

    private boolean isMessageIdDuplicate(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current.getMessage() != null
                    && current.getMessage().contains("uk_product_views_message_id")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
