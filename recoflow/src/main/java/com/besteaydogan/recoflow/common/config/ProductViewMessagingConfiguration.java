package com.besteaydogan.recoflow.common.config;

import java.time.Clock;
import java.util.concurrent.TimeUnit;

import com.besteaydogan.recoflow.messaging.producer.PublicationDelay;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration(proxyBeanMethods = false)
public class ProductViewMessagingConfiguration {

    @Bean
    NewTopic productViewsTopic(ProductViewKafkaProperties properties) {
        return TopicBuilder.name(properties.productViewsTopic())
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    Clock utcClock() {
        return Clock.systemUTC();
    }

    @Bean
    PublicationDelay publicationDelay() {
        return duration -> TimeUnit.NANOSECONDS.sleep(duration.toNanos());
    }
}
