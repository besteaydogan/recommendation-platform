package com.besteaydogan.recoflow.common.config;

import java.time.Duration;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "recoflow.kafka")
public record ProductViewKafkaProperties(
        @NotBlank String productViewsTopic,
        @Positive int retryMaxAttempts,
        @NotNull Duration retryBackoff
) {

    public String productViewsDltTopic() {
        return productViewsTopic + ".DLT";
    }

    @AssertTrue(message = "retry backoff must not be negative")
    public boolean isRetryBackoffValid() {
        return retryBackoff != null && !retryBackoff.isNegative();
    }
}
