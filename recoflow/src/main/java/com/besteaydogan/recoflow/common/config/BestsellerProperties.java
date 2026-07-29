package com.besteaydogan.recoflow.common.config;

import java.time.Duration;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "recoflow.bestseller")
public record BestsellerProperties(
        boolean refreshEnabled,
        @NotNull Duration refreshInterval
) {

    @AssertTrue(message = "refresh interval must be positive when refresh is enabled")
    public boolean isRefreshIntervalValid() {
        return !refreshEnabled
                || (refreshInterval != null && !refreshInterval.isZero() && !refreshInterval.isNegative());
    }
}
