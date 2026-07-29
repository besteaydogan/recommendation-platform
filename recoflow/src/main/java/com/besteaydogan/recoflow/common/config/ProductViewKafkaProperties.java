package com.besteaydogan.recoflow.common.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "recoflow.kafka")
public record ProductViewKafkaProperties(
        @NotBlank String productViewsTopic
) {
}
