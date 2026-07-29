package com.besteaydogan.recoflow.messaging.producer;

import java.time.Duration;

@FunctionalInterface
public interface PublicationDelay {

    void pause(Duration duration) throws InterruptedException;
}
