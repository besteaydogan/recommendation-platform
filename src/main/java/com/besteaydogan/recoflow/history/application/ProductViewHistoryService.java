package com.besteaydogan.recoflow.history.application;

import com.besteaydogan.recoflow.history.infrastructure.ProductView;
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
        if (repository.existsByMessageId(event.messageId())) {
            return false;
        }

        repository.saveAndFlush(new ProductView(
                event.messageId(),
                event.userId(),
                event.properties().productId(),
                event.context().source(),
                event.viewedAt()
        ));
        return true;
    }
}
