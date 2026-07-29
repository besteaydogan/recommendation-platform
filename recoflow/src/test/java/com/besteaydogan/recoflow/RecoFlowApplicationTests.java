package com.besteaydogan.recoflow;

import com.besteaydogan.recoflow.history.infrastructure.ProductViewRepository;
import com.besteaydogan.recoflow.history.infrastructure.TopCategoryQueryRepository;
import com.besteaydogan.recoflow.messaging.model.ProductViewEvent;
import com.besteaydogan.recoflow.messaging.producer.ProductViewProducerRunner;
import com.besteaydogan.recoflow.recommendation.infrastructure.BestsellerQueryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
class RecoFlowApplicationTests {

    @Autowired
    private ApplicationContext applicationContext;

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
}
