package com.besteaydogan.recoflow.common.config;

import java.time.Clock;
import java.util.concurrent.TimeUnit;

import com.besteaydogan.recoflow.common.observability.RecoFlowMetrics;
import com.besteaydogan.recoflow.messaging.model.ProductViewEvent;
import com.besteaydogan.recoflow.messaging.producer.PublicationDelay;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.RetryListener;
import org.springframework.util.backoff.FixedBackOff;

@Configuration(proxyBeanMethods = false)
public class ProductViewMessagingConfiguration {

    @Bean
    NewTopic productViewsTopic(ProductViewKafkaProperties properties) {
        return TopicBuilder.name(properties.productViewsTopic())
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    NewTopic productViewsDltTopic(ProductViewKafkaProperties properties) {
        return TopicBuilder.name(properties.productViewsDltTopic())
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    DefaultErrorHandler productViewErrorHandler(
            KafkaTemplate<String, ProductViewEvent> kafkaTemplate,
            ProductViewKafkaProperties properties,
            RecoFlowMetrics metrics
    ) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, exception) -> new TopicPartition(
                        properties.productViewsDltTopic(),
                        record.partition()
                )
        );
        recoverer.setFailIfSendResultIsError(true);
        long retryCount = properties.retryMaxAttempts() - 1L;
        FixedBackOff backOff = new FixedBackOff(properties.retryBackoff().toMillis(), retryCount);
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);
        errorHandler.setRetryListeners(new RetryListener() {
            @Override
            public void failedDelivery(ConsumerRecord<?, ?> record, Exception exception, int deliveryAttempt) {
                // Consumer failures are counted at the listener boundary.
            }

            @Override
            public void recovered(ConsumerRecord<?, ?> record, Exception exception) {
                metrics.kafkaEventSentToDlt();
            }
        });
        return errorHandler;
    }

    @Bean
    Clock utcClock() {
        return Clock.systemUTC();
    }

    @Bean
    PublicationDelay publicationDelay() {
        return duration -> TimeUnit.NANOSECONDS.sleep(duration.toNanos());
    }
}
