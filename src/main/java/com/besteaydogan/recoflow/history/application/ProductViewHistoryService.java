package com.besteaydogan.recoflow.history.application;

import com.besteaydogan.recoflow.history.infrastructure.ProductViewRepository;
import com.besteaydogan.recoflow.messaging.model.ProductViewEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductViewHistoryService {

    private final ProductViewRepository repository;

    public ProductViewHistoryService(ProductViewRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public boolean record(ProductViewEvent event) {
        return repository.insertIfAbsent(
                event.messageId(),
                event.userId(),
                event.properties().productId(),
                event.context().source(),
                event.viewedAt()
        ) == 1;
    }
}
