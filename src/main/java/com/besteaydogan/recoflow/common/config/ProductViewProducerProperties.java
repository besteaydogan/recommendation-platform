package com.besteaydogan.recoflow.common.config;

import java.time.Duration;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "recoflow.producer")
public record ProductViewProducerProperties(
        boolean enabled,
        @NotBlank String filePath,
        @NotNull Duration interval
) {

    @AssertTrue(message = "interval must be positive")
    public boolean isIntervalPositive() {
        return interval != null && !interval.isZero() && !interval.isNegative();
    }
}
