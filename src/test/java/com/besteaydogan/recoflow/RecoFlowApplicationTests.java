package com.besteaydogan.recoflow;

import com.besteaydogan.recoflow.common.observability.RecoFlowMetrics;
import com.besteaydogan.recoflow.history.infrastructure.ProductViewRepository;
import com.besteaydogan.recoflow.history.infrastructure.TopCategoryQueryRepository;
import com.besteaydogan.recoflow.messaging.model.ProductViewEvent;
import com.besteaydogan.recoflow.messaging.producer.ProductViewProducerRunner;
import com.besteaydogan.recoflow.recommendation.infrastructure.BestsellerQueryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(properties = "management.prometheus.metrics.export.enabled=true")
@AutoConfigureMockMvc
class RecoFlowApplicationTests {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RecoFlowMetrics metrics;

    @MockitoBean
    private ProductViewRepository productViewRepository;

    @MockitoBean
    private KafkaTemplate<String, ProductViewEvent> kafkaTemplate;

    @MockitoBean
    private TopCategoryQueryRepository topCategoryQueryRepository;

    @MockitoBean
    private BestsellerQueryRepository bestsellerQueryRepository;

    @Test
    void contextLoads() {
    }

    @Test
    void producerRunnerIsAbsentWhenProducerIsDisabled() {
        assertThat(applicationContext.getBeansOfType(ProductViewProducerRunner.class)).isEmpty();
    }

    @Test
    void exposesApplicationMetricsInPrometheusFormat() throws Exception {
        metrics.kafkaEventConsumed();
        metrics.kafkaConsumerFailed();
        metrics.kafkaEventSentToDlt();
        metrics.recordRecommendation(() -> "ok");

        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "recoflow_kafka_consumer_events_total"
                )))
                .andExpect(content().string(containsString(
                        "recoflow_kafka_consumer_failures_total"
                )))
                .andExpect(content().string(containsString(
                        "recoflow_kafka_dlt_messages_total"
                )))
                .andExpect(content().string(containsString(
                        "recoflow_recommendation_requests_total"
                )))
                .andExpect(content().string(containsString(
                        "recoflow_recommendation_latency_seconds_count"
                )));
    }
}
