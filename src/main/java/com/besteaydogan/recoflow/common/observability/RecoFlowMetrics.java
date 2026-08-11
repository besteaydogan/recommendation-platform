package com.besteaydogan.recoflow.common.observability;

import java.util.function.Supplier;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class RecoFlowMetrics {

    public static final String KAFKA_CONSUMER_EVENTS = "recoflow.kafka.consumer.events";
    public static final String KAFKA_CONSUMER_FAILURES = "recoflow.kafka.consumer.failures";
    public static final String KAFKA_DLT_MESSAGES = "recoflow.kafka.dlt.messages";
    public static final String RECOMMENDATION_REQUESTS = "recoflow.recommendation.requests";
    public static final String RECOMMENDATION_LATENCY = "recoflow.recommendation.latency";

    private final Counter kafkaConsumerEvents;
    private final Counter kafkaConsumerFailures;
    private final Counter kafkaDltMessages;
    private final Counter recommendationRequests;
    private final Timer recommendationLatency;

    public RecoFlowMetrics(MeterRegistry registry) {
        this.kafkaConsumerEvents = Counter.builder(KAFKA_CONSUMER_EVENTS)
                .description("Kafka records delivered to the product-view consumer, including retries")
                .register(registry);
        this.kafkaConsumerFailures = Counter.builder(KAFKA_CONSUMER_FAILURES)
                .description("Product-view consumer processing attempts that ended with an exception")
                .register(registry);
        this.kafkaDltMessages = Counter.builder(KAFKA_DLT_MESSAGES)
                .description("Product-view records successfully recovered to the dead-letter topic")
                .register(registry);
        this.recommendationRequests = Counter.builder(RECOMMENDATION_REQUESTS)
                .description("Recommendation requests handled by the application service")
                .register(registry);
        this.recommendationLatency = Timer.builder(RECOMMENDATION_LATENCY)
                .description("Recommendation request processing latency")
                .publishPercentileHistogram()
                .register(registry);
    }

    public void kafkaEventConsumed() {
        kafkaConsumerEvents.increment();
    }

    public void kafkaConsumerFailed() {
        kafkaConsumerFailures.increment();
    }

    public void kafkaEventSentToDlt() {
        kafkaDltMessages.increment();
    }

    public <T> T recordRecommendation(Supplier<T> operation) {
        recommendationRequests.increment();
        return recommendationLatency.record(operation);
    }
}
