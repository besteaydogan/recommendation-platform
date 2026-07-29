package com.besteaydogan.recoflow.messaging.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.besteaydogan.recoflow.history.api.BrowsingHistoryResponse;
import com.besteaydogan.recoflow.recommendation.api.RecommendationResponse;
import com.besteaydogan.recoflow.recommendation.api.RecommendationType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductViewEventAndResponseJsonTests {

    private final ObjectMapper objectMapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .build();

    @Test
    void deserializesAssignmentProductViewEvent() throws Exception {
        String json = """
                {
                  "event": "ProductView",
                  "messageid": "c002a71a-9750-4604-8d70-d5ff3f1c4495",
                  "userid": "user-120",
                  "properties": {
                    "productid": "product-393"
                  },
                  "context": {
                    "source": "mobile-app"
                  }
                }
                """;

        ProductViewEvent event = objectMapper.readValue(json, ProductViewEvent.class);

        assertThat(event.event()).isEqualTo("ProductView");
        assertThat(event.messageId()).isEqualTo(UUID.fromString("c002a71a-9750-4604-8d70-d5ff3f1c4495"));
        assertThat(event.userId()).isEqualTo("user-120");
        assertThat(event.properties().productId()).isEqualTo("product-393");
        assertThat(event.context().source()).isEqualTo("mobile-app");
        assertThat(event.viewedAt()).isNull();
    }

    @Test
    void serializesProductViewEventWithTimestamp() throws Exception {
        Instant viewedAt = Instant.parse("2026-07-29T09:30:00Z");
        ProductViewEvent event = new ProductViewEvent(
                "ProductView",
                UUID.fromString("c002a71a-9750-4604-8d70-d5ff3f1c4495"),
                "user-120",
                new ProductViewEvent.ProductProperties("product-393"),
                new ProductViewEvent.EventContext("mobile-app"),
                viewedAt
        );

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(event));

        assertThat(json.get("messageid").asText()).isEqualTo(event.messageId().toString());
        assertThat(json.get("userid").asText()).isEqualTo("user-120");
        assertThat(json.at("/properties/productid").asText()).isEqualTo("product-393");
        assertThat(json.at("/context/source").asText()).isEqualTo("mobile-app");
        assertThat(json.get("viewedat").asText()).isEqualTo("2026-07-29T09:30:00Z");
        assertThat(json.has("messageId")).isFalse();
        assertThat(json.has("viewedAt")).isFalse();
    }

    @Test
    void serializesBrowsingHistoryResponseFieldNames() throws Exception {
        BrowsingHistoryResponse response = new BrowsingHistoryResponse(
                "user-120",
                List.of("product-10", "product-20"),
                "personalized"
        );

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(response));

        assertThat(json.get("user-id").asText()).isEqualTo("user-120");
        assertThat(json.get("products").size()).isEqualTo(2);
        assertThat(json.get("products").get(0).asText()).isEqualTo("product-10");
        assertThat(json.get("type").asText()).isEqualTo("personalized");
        assertThat(json.has("userId")).isFalse();
    }

    @Test
    void serializesRecommendationResponseType() throws Exception {
        RecommendationResponse response = new RecommendationResponse(
                "user-120",
                List.of("product-1", "product-2"),
                RecommendationType.NON_PERSONALIZED
        );

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(response));

        assertThat(json.get("user-id").asText()).isEqualTo("user-120");
        assertThat(json.get("type").asText()).isEqualTo("non-personalized");
    }
}
