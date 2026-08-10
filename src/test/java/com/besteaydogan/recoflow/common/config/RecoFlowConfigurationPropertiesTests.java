package com.besteaydogan.recoflow.common.config;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class RecoFlowConfigurationPropertiesTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfiguration.class);

    @Test
    void bindsValidConfiguration() {
        validContext()
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(ProductViewKafkaProperties.class).productViewsTopic())
                            .isEqualTo("product-views");
                    assertThat(context.getBean(ProductViewKafkaProperties.class).productViewsDltTopic())
                            .isEqualTo("product-views.DLT");
                    assertThat(context.getBean(ProductViewKafkaProperties.class).retryMaxAttempts())
                            .isEqualTo(3);
                    assertThat(context.getBean(ProductViewKafkaProperties.class).retryBackoff())
                            .isEqualTo(Duration.ofMillis(250));
                    assertThat(context.getBean(ProductViewProducerProperties.class).filePath())
                            .isEqualTo("/app/data/product-views.json");
                    assertThat(context.getBean(ProductViewProducerProperties.class).enabled()).isFalse();
                    assertThat(context.getBean(ProductViewProducerProperties.class).interval())
                            .isEqualTo(Duration.ofMillis(750));
                    assertThat(context.getBean(BestsellerProperties.class).refreshEnabled()).isTrue();
                    assertThat(context.getBean(BestsellerProperties.class).refreshInterval())
                            .isEqualTo(Duration.ofSeconds(30));
                });
    }

    @Test
    void rejectsBlankKafkaTopic() {
        validContext("recoflow.kafka.product-views-topic= ")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(BindValidationException.class)
                            .hasStackTraceContaining("productViewsTopic");
                });
    }

    @Test
    void rejectsNonPositiveProducerInterval() {
        validContext("recoflow.producer.interval=0ms")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(BindValidationException.class)
                            .hasStackTraceContaining("interval must be positive");
                });
    }

    @Test
    void rejectsNonPositiveRefreshIntervalWhenEnabled() {
        validContext("recoflow.bestseller.refresh-interval=0ms")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(BindValidationException.class)
                            .hasStackTraceContaining("refresh interval must be positive when refresh is enabled");
                });
    }

    private ApplicationContextRunner validContext(String... overrides) {
        return contextRunner
                .withPropertyValues(
                        "recoflow.kafka.product-views-topic=product-views",
                        "recoflow.kafka.retry-max-attempts=3",
                        "recoflow.kafka.retry-backoff=250ms",
                        "recoflow.producer.enabled=false",
                        "recoflow.producer.file-path=/app/data/product-views.json",
                        "recoflow.producer.interval=750ms",
                        "recoflow.bestseller.refresh-enabled=true",
                        "recoflow.bestseller.refresh-interval=30s")
                .withPropertyValues(overrides);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties({
            ProductViewKafkaProperties.class,
            ProductViewProducerProperties.class,
            BestsellerProperties.class
    })
    static class PropertiesConfiguration {
    }
}
