package com.besteaydogan.recoflow.recommendation.application;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.boot.context.event.ApplicationReadyEvent;

@Component
@ConditionalOnProperty(
        prefix = "recoflow.bestseller",
        name = "refresh-enabled",
        havingValue = "true"
)
public class BestsellerRefreshScheduler {

    private final BestsellerRefreshService refreshService;

    public BestsellerRefreshScheduler(BestsellerRefreshService refreshService) {
        this.refreshService = refreshService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void refreshAtStartup() {
        refreshService.refreshIfEnabled();
    }

    @Scheduled(
            fixedDelayString = "${recoflow.bestseller.refresh-interval}",
            initialDelayString = "${recoflow.bestseller.refresh-interval}"
    )
    public void refreshPeriodically() {
        refreshService.refreshIfEnabled();
    }
}
