package com.besteaydogan.recoflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class RecoFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(RecoFlowApplication.class, args);
    }
}
