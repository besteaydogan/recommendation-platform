package com.besteaydogan.recoflow.messaging.model;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProductViewEvent(
        @NotBlank String event,
        @JsonProperty("messageid") @NotNull UUID messageId,
        @JsonProperty("userid") @NotBlank String userId,
        @NotNull @Valid ProductProperties properties,
        @NotNull @Valid EventContext context,
        @JsonProperty("viewedat") @JsonFormat(shape = JsonFormat.Shape.STRING) @NotNull Instant viewedAt
) {

    public record ProductProperties(
            @JsonProperty("productid") @NotBlank String productId
    ) {
    }

    public record EventContext(
            @NotBlank String source
    ) {
    }
}
